package io.github.sandydunlop.markista.spi;

import java.security.Provider.Service;

import io.github.sandydunlop.markista.core.Context;
import io.github.sandydunlop.markista.model.Api;



/// This interface defines a service for processing a project's structure
/// and documentation with the 
/// [Markista markdown doclet](https://sandydunlop.github.io/markista/). 
/// The DocService interface is part of the
/// [Service Provider Interface](https://docs.oracle.com/javase/tutorial/sound/SPI-intro.html)
/// (SPI) design pattern, which allows
/// for the dynamic discovery and loading of service implementations at runtime.
///
/// Implementations of this interface can be created to provide various
/// algorithms or strategies for processing project structures and documentation.
/// By adhering to this SPI, developers can extend the functionality of Markista
/// without modifying its core codebase, promoting a modular and flexible architecture.
///
/// Additionally, this interface is designed to work seamlessly with the
/// [Java Platform Module System](https://openjdk.org/jeps/261) (JPMS), which 
/// enhances the modularity of Java
/// applications. JPMS allows for better encapsulation and management of dependencies,
/// enabling developers to create well-defined modules that can interact with
/// the Markista framework. This modular approach facilitates the integration of
/// third-party services and custom implementations, ensuring that they can be
/// easily added or replaced as needed.
///
/// When the structure (including documentation) is processed by Markista,
/// it is subsequently passed into the DocService's `run` method, where
/// custom processing logic can be executed. This design makes it easier to 
/// maintain and extend the documentation processing capabilities of Markista.
public interface DocService {
    /// Returns true if this Docservice replaces the default DocService.
    /// @return true if this DocService replaces the default Markdown one, false otherwise.
    boolean replacesDefault();
    
    /// Runs before the main Markdown DocService.
    /// Initializes a DocService with the API model and a Context for file creation and reporting.
    /// @param api the Api object representing the project's structure
    /// @param ctx the Context object providing additional information for processing
    /// @return true on success, otheriwse false
    boolean start(Api api, Context ctx);

    /// Runs after the main Markdown DocService.
    /// @return true on success, otheriwse false
    boolean finish();
}
