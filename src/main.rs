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

use kuchiki::{Html, NodeRef};
use kuchiki::iter::{Select, Elements, Descendants};

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

    let mut links = check_readme(&repo);
    for link in check_wiki(&repo) {
        links.insert(link);
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

    fn wiki_url(&self) -> Url {
        Url::parse(&format!("{}/wiki", self.url().serialize())[..]).unwrap()
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
    page_title: String,
}
impl fmt::Display for CheckedLink {
    fn fmt(&self, f: &mut fmt::Formatter) -> fmt::Result {
        if self.text != "" {
            write!(f, "([{}]({}), page=[{}]({}), broken={})",
                   self.text, self.url, self.page_title, self.referrer, self.broken)
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
    match client.get(&url.serialize()).send() {
        Ok(res) => !res.status.is_success(),
        Err(_) => false,
    }
}
fn get_doc(url: &Url) -> NodeRef {
    let client = Client::new();
    let mut res = client.get(&url.serialize()).send().unwrap();
    let html = Html::from_stream(&mut res).unwrap();
    html.parse()
}

fn check_readme(repo: &Repo) -> HashSet<CheckedLink> {
    let doc = get_doc(&repo.url());
    let select = doc.select("#readme a");
    check_links(select, repo.url(), String::from("README"))
}

fn check_wiki(repo: &Repo) -> HashSet<CheckedLink> {
    let wiki_home = get_doc(&repo.wiki_url());
    let mut links = HashSet::new();

    for page_link in wiki_home.select("a.wiki-page-link").unwrap(){
        let node = page_link.as_node();
        let el = node.as_element().unwrap();
        let attrs = el.attributes.borrow();
        let href = attrs.get(&qualname!("", "href")).unwrap();

        let page_title = node.text_contents();
        let page_url = UrlParser::new()
            .base_url(&repo.wiki_url()).parse(href).unwrap();
        let page = get_doc(&page_url);

        for link in
            check_links(
                page.select("div.markdown-body a"),
                page_url,
                page_title.clone(),
            )
        {
            links.insert(link);
        }
    };

    links
}

type SelectResult = Result<Select<Elements<Descendants>>,()>;

fn check_links(select_result: SelectResult, referrer: Url, page_title: String) -> HashSet<CheckedLink> {
    let mut links = HashSet::new();
    if select_result.is_err() {
        return links;
    }

    for a in select_result.unwrap() {
        let node = a.as_node();
        let el = node.as_element().unwrap();
        let attrs = el.attributes.borrow();
        let href = attrs.get(&qualname!("", "href")).unwrap();

        let url = {
            let parsed_url = UrlParser::new().base_url(&referrer).parse(href);
            if parsed_url.is_err() {
                info!("Invalid URL on {}: {}", page_title, node.text_contents());
                continue;
            }
            parsed_url.unwrap()
        };

        let mut link = CheckedLink {
            url: url.clone(),
            broken: false,
            referrer: referrer.clone(),
            text: node.text_contents(),
            page_title: page_title.clone()
        };

        if !links.contains(&link) {
            link.broken = is_broken(&url);
            links.insert(link);
        }
    }
    links
}
