# GraphQLviz

GraphQLviz marries GraphQL (schemas) with Graphviz.

## Usage

You can use GraphQLviz from command-line if you have [Graphviz](http://www.graphviz.org) and [Leiningen](http://leiningen.org) like this.

```
lein run examples/digitransit.json digitransit
```

Also there is a prepackaged jar in [Clojars](http://clojars.org) that is built with.
```
lein uberjar
```

You can use it like a regular Java app like this.

```
java -jar graphqlviz.jar examples/digitransit.json digitransit
```

TODO: The next verision will also support doing introspection queries.

![Example schema](examples/digitransit.json?raw=true) (from [Digitransit](http://digitransit.fi))

![Example graph](https://rawgit.com/Macroz/GraphQLviz/master/examples/digitransit.svg)

## Backlog

- Add introspection query
- Use dynamic graph features as in [archi](https://github.com/Macroz/archi)

=======
## License

Copyright © 2015 Markku Rontu

Distributed under the Eclipse Public License either version 1.0 or (at
your option) any later version.
