# Web application for playing Sudoku games

This project is one of infinitely many projects for playing Sudoku games.
At the same time, this project is used to try out JPA/Hibernate and jOOQ using a non-trivial database.
That is, the transactional services in this project have 2 implementations, one using JPA
and one using jOOQ.

Make sure to start the PostgreSQL Docker container before starting the application.
The database is called "sudoku", but other than that see [sample Pagila DB](https://github.com/devrimgunduz/pagila/tree/master) for how to
initialize the database using Docker.
