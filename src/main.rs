#[macro_use]
extern crate string_cache;
extern crate hyper;
extern crate kuchiki;
extern crate url;

use std::env::args;
use std::fmt;

use hyper::Client;

use kuchiki::Html;

use url::{Url, UrlParser, ParseError};

fn main() {
    let arg = args().last().unwrap();
    let repo = {
        let mut split = arg.split('/');
        Repo {
            owner: split.next().unwrap().to_string(),
            name: split.next().unwrap().to_string()
        }
    };

    for url in check_readme(repo) {
        println!("{}", url);
    }
}

struct Repo {
    owner: String,
    name: String
}
impl Repo {
    fn url(&self) -> String {
        format!("https://github.com/{}/{}", self.owner, self.name)
    }
}
impl fmt::Display for Repo {
    fn fmt(&self, f: &mut fmt::Formatter) -> fmt::Result {
        write!(f, "{}/{}", self.owner, self.name)
    }
}

fn check_readme(repo: Repo) -> Vec<Url> {
    let client = Client::new();
    let readmeUrl = Url::parse(&repo.url()[..]).unwrap();
    let mut res = client.get(&repo.url()[..]).send().unwrap();
    let html = Html::from_stream(&mut res).unwrap();
    let doc = html.parse();

    let mut ret: Vec<Url> = Vec::new();
    for a in doc.select("#readme a").unwrap() {
        let node = a.as_node();
        let el = node.as_element().unwrap();
        let attrs = el.attributes.borrow();
        let href = attrs.get(&qualname!("", "href")).unwrap();
        let url = UrlParser::new().base_url(&readmeUrl)
            .parse(href).unwrap();
        ret.push(url);
    }
    ret
}
