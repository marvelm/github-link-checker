# github-link-checker

Tested with Rust 1.4.0 stable
Links that take forever to resolve will break the checker because support for Tcp timeouts haven't been implemented.

Usage:
>./github-link-checker marvelm/github-link-checker

### How to build and run
>git clone https://github.com/marvelm/github-link-checker.git

>cd github-link-checker

>cargo build

>./target/debug/github-link-checker hyperium/hyper
