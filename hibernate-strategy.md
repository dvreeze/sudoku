
# A strategy for successfully building applications using Hibernate and jOOQ

Building performant and maintainable applications using [Hibernate ORM](https://hibernate.org/orm/)
can be hard. At least that has been my experience for a long time. To be honest, I did not understand
Hibernate sufficiently well, and neither did many of my co-workers.

At some point, with the help of resources such as
[Getting Started with Hibernate](https://docs.hibernate.org/orm/7.1/quickstart/html_single/) I started
to "get" Hibernate. The general idea is *working with the database rather than against it*. Too often
I have seen attempts to "abstract away" the database using Hibernate (or other ORMs), which is
counterproductive, leading to non-performant, brittle, hard to maintain code.

That said, Hibernate/JPA *entities* are quite poor *data transfer objects*, to be passed across
application layers. Their mutability and use of proxy objects is a strength within an open
JPA `EntityManager` (or Hibernate `Session`), but a liability when passing around "data objects"
through the application (from service layer to web layer, for example).

A successful strategy for using Hibernate could therefore be as follows:
* Within an open `EntityManager`, use Hibernate to work *with the database* as much as possible
  * See good resources on effective use of Hibernate in order to be successful
* Outside the `EntityManager`/`Session`, pass around data as *deeply immutable record class instances*
  * See for example the book Effective Java, 3rd Edition, by Joshua Bloch, on *minimizing mutability*

The underlying paradigms involved are quite different:
* Within an open `EntityManager`: JPA entities as old-school mutable JavaBeans, using `null` everywhere
* Outside the `EntityManager`: immutable Java records, avoiding `null`, using (type-safe) `Optional` instead

In this and other projects I explore these ideas. Combining seemingly conflicting ideas in the same
code base can be supported by the use of clear Javadoc comments in `package-info.java` source files.

## Using Hibernate in tandem with the database

## Passing data around as immutable Java records
