# GraphQLviz [![Build Status](https://travis-ci.org/Macroz/GraphQLviz.svg?branch=master)](https://travis-ci.org/Macroz/GraphQLViz)

GraphQLviz marries GraphQL (schemas) with Graphviz.

## Usage

You can use GraphQLviz from command-line if you have [Graphviz](http://www.graphviz.org) and [Leiningen](http://leiningen.org) like this.

```
lein run examples/digitransit.json digitransit
```

Also there is a prepackaged jar in [Clojars](https://clojars.org/macroz/graphqlviz) that is built with.
```
lein uberjar
```

You can use it like a regular Java app like this.

```
java -jar graphqlviz.jar examples/digitransit.json digitransit
```

![Example schema](examples/digitransit.json?raw=true) (from [Digitransit](http://digitransit.fi))

![Example graph](https://rawgit.com/Macroz/GraphQLviz/master/examples/digitransit.svg)

To run against a live GraphQL server, you can use the introspection query like this.

```
java -jar graphqlviz.jar http://digitransit.fi/otp/routers/finland/index/graphql digitransit
```

Also if you want to use the code from Clojure, add to your project.clj:

[![Clojars Project](http://clojars.org/macroz/graphqlviz/latest-version.svg)](http://clojars.org/macroz/graphqlviz)


## Backlog

- Use dynamic graph features as in [archi](https://github.com/Macroz/archi)

=======
## License

Copyright © 2015 Markku Rontu

Distributed under the Eclipse Public License either version 1.0 or (at
your option) any later version.
