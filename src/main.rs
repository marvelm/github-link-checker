#[macro_use]
extern crate string_cache;
extern crate hyper;
extern crate kuchiki;
extern crate url;

use std::env::args;
use std::fmt;
use std::collections::HashSet;

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

    for link in check_readme(repo) {
        println!("{}", link);
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

#[derive(Hash, Debug)]
struct CheckedLink {
    url: Url,
    broken: bool
}
impl fmt::Display for CheckedLink {
    fn fmt(&self, f: &mut fmt::Formatter) -> fmt::Result {
        write!(f, "({}, broken={})", self.url, self.broken)
    }
}
impl std::cmp::PartialEq for CheckedLink {
    fn eq(&self, other: &CheckedLink) -> bool {
       self.url.serialize_no_fragment() == other.url.serialize_no_fragment()
    }
    fn ne(&self, other: &CheckedLink) -> bool {
        !self.eq(other)
    }
}
impl std::cmp::Eq for CheckedLink {}

fn is_broken(url: Url) -> bool {
    let client = Client::new();
    match client.get(&url.serialize()[..]).send() {
        Ok(res) => !res.status.is_success(),
        Err(_) => false,
    }
}

fn check_readme(repo: Repo) -> HashSet<CheckedLink> {
    let client = Client::new();
    let readmeUrl = Url::parse(&repo.url()[..]).unwrap();
    let mut res = client.get(&repo.url()[..]).send().unwrap();
    let html = Html::from_stream(&mut res).unwrap();
    let doc = html.parse();

    let mut links = HashSet::new();
    for a in doc.select("#readme a").unwrap() {
        let node = a.as_node();
        let el = node.as_element().unwrap();
        let attrs = el.attributes.borrow();
        let href = attrs.get(&qualname!("", "href")).unwrap();
        let url = UrlParser::new().base_url(&readmeUrl)
            .parse(href).unwrap();
        links.insert(CheckedLink{
            url: url.clone(),
            broken: is_broken(url)
        });
    }
    links
}
