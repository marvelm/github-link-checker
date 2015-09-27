# github-link-checker
Links that take forever to resolve will break the checker
because the Stable release of Rust doesn't have support for Tcp timeouts.

Usage:
>./github-link-checker marvelm/github-link-checker

### How to build and run
>git clone https://github.com/marvelm/github-link-checker.git

>cd github-link-checker

>cargo build

>./target/debug/github-link-checker hyperium/hyper
