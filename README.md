# aggregate

## Notes on High-Level Interface

The high-level `Iteration` interface allows zipping and aggregating arbitrary sources,
with options to complete some of the sources with `None` values in order to accommodate for different sizes

There are many design choices (such as whether to make `xs.current` an `Option` by default or not),
but the core of the issue is in making `flatMap` able to be separated into two stages; i.e., binding times,
so that the iteration can be first set up and _then_ run,
after initializing the state for each particular run. 

Two major roadblocks to this:

 * there should be a way to check the by-name argument of a call to `flatMap` is not evaluated too early;
   
   a good approximation may be achieved by making `flatMap` a blackbox macro that checks that its argument is only evaluated as part of some lambda or by-name expression, and not immediately;
   
   this is an approximation because we could still make it crash by putting the access in a redex or even calling `run` in theb wrong place, for example, but it should remove the vats majority of common user mistakes

 * pattern matching in `flatMap` cannot be supported as is, since it evaluates its argument immediately;
   
   this limitation is expected, since without it we could shape the computation based on the shape of the dynamic argument, which is a binding time violation;
   
   fortunately, there is a simple thing that the `flatMap` blackbox macro could do to avoid the problem:
   transform `xs.flatMap{case P => E}` into `xs.flatMap{p => lazy val P = p; E}`,
   and then let the laziness checker mentioned above make sure
   references to the variables bound in `P` do not trigger an evaluation of `p` too early 

 * Scala's [convoluted desugaring](https://github.com/lampepfl/dotty/issues/2573) of `for` comprehensions introduces needless intermediate `map`'s,
  often ripe with synthetic pattern matching used to deal with tuples that are introduced by the desugaring... which triggers the problem above
  
  again, this could be solved with a (perhaps rather intricate, this time) blackbox macro


