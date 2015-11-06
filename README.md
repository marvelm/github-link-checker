# github-link-checker

Tested with Rust 1.4.0 stable

Links that take longer than 5 seconds to resolve will be marked as broken.

Usage:
>./github-link-checker marvelm/github-link-checker

### How to build and run
>git clone https://github.com/marvelm/github-link-checker.git

>cd github-link-checker

>cargo build

>./target/debug/github-link-checker hyperium/hyper
