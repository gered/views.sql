# views-sql

SQL plugin for the [views][1] library. Allows for plain SQL strings to 
be used.

[1]: https://github.com/gered/views 

Implementation is largely based on the views-honeysql library.

Note that this library leverages [JSqlParser][2] for parsing SQL
queries and extracting the view system hint information needed.
JSqlParser is not perfect and will not be able to parse some more
complex queries and/or queries using some vendor-specific extensions.

[2]: https://github.com/JSQLParser/JSqlParser

## License

Copyright © 2016 Gered King

Distributed under the MIT License.
