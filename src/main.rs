#[macro_use]
extern crate string_cache;
extern crate hyper;
extern crate kuchiki;
extern crate url;

#[macro_use]
extern crate log;

use std::env::args;
use std::fmt;
use std::collections::HashSet;

use hyper::Client;

use kuchiki::Html;

use url::{Url, UrlParser};

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
        if link.broken {
            println!("{}", link);
        }
    }
}

struct Repo {
    owner: String,
    name: String,
}
impl Repo {
    fn url(&self) -> Url {
        Url::parse(&format!(
            "https://github.com/{}/{}", self.owner, self.name)[..]).unwrap()
    }
}
impl fmt::Display for Repo {
    fn fmt(&self, f: &mut fmt::Formatter) -> fmt::Result {
        write!(f, "{}/{}", self.owner, self.name)
    }
}

#[derive(Debug)]
struct CheckedLink {
    url: Url,
    broken: bool,
    referrer: Url,
    text: String,
}
impl fmt::Display for CheckedLink {
    fn fmt(&self, f: &mut fmt::Formatter) -> fmt::Result {
        if self.text != "" {
            write!(f, "([{}]({}), page={}, broken={})",
                   self.text, self.url, self.referrer, self.broken)
        } else {
            write!(f, "({}, page={}, broken={})",
                   self.url, self.referrer, self.broken)
        }
    }
}
impl std::cmp::PartialEq for CheckedLink {
    fn eq(&self, other: &CheckedLink) -> bool {
        self.url.serialize_no_fragment() == other.url.serialize_no_fragment()
            && self.referrer.serialize_no_fragment() == other.referrer.serialize_no_fragment()
    }
}
impl std::hash::Hash for CheckedLink {
    fn hash<H>(&self, state: &mut H) where H: std::hash::Hasher {
        let mut bytes = self.url.serialize_no_fragment().into_bytes();
        let referrer = self.referrer.serialize_no_fragment().into_bytes();
        bytes.extend(referrer);
        state.write(&bytes[..]);
    }
}
impl std::cmp::Eq for CheckedLink {}

fn is_broken(url: &Url) -> bool {
    let client = Client::new();
    match client.get(&url.serialize()[..]).send() {
        Ok(res) => !res.status.is_success(),
        Err(_) => false,
    }
}

fn check_readme(repo: Repo) -> HashSet<CheckedLink> {
    let client = Client::new();
    let readme_url = repo.url();
    let mut res = client.get(&readme_url.serialize()).send().unwrap();
    let html = Html::from_stream(&mut res).unwrap();
    let doc = html.parse();

    let mut links = HashSet::new();
    let select = doc.select("#readme a");
    if select.is_err() {
        return links;
    }

    for a in select.unwrap() {
        let node = a.as_node();
        let el = node.as_element().unwrap();
        let attrs = el.attributes.borrow();
        let href = attrs.get(&qualname!("", "href")).unwrap();

        let url = {
            let parsed_url = UrlParser::new().base_url(&readme_url).parse(href);
            if parsed_url.is_err() {
                info!("Invalid URL on README: {}", node.text_contents());
                continue;
            }
            parsed_url.unwrap()
        };

        let mut link = CheckedLink {
            url: url.clone(),
            broken: false,
            referrer: readme_url.clone(),
            text: node.text_contents(),
        };

        if !links.contains(&link) {
            link.broken = is_broken(&url);
            links.insert(link);
        }
    }
    links
}
