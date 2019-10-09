FROM rust:latest

WORKDIR /usr/src/myapp
COPY . .

RUN rustc dltree.ds

CMD ["dltree"]
