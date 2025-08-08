package io.github.sandydunlop.markista.model;


/// Enum representing Java language modifiers with their string 
/// representations. 
/// 
public enum Modifier {
    /// The public modifier. 
    PUBLIC("public"), 

    /// The protected modifier. 
    PROTECTED("protected"), 
    
    /// The private modifier. 
    PRIVATE("private"), 
    
    /// The static modifier.
    STATIC("static"), 

    /// The final modifier.
    FINAL("final"), 
    
    /// The abstract modifier. 
    ABSTRACT("abstract"),
    
    /// The synchronized modifier. 
    SYNCHRONIZED("synchronized"),
    
    /// The transient modifier. 
    TRANSIENT("transient"), 
    
    /// The volatile modifier.
    VOLATILE("volatile"),
    
    /// The native modifier. 
    NATIVE("native"),
    
    /// The strictfp modifier. 
    STRICTFP("strictfp"),
    
    /// The default modifier. 
    DEFAULT("default");


    /// The string representation of the modifier.
    private final String name;

    /// Constructs a Modifier enum constant with the specified string value.
    /// @param s The string representation of the modifier.
    Modifier(String s) {
        name = s;
    }

    /// Returns the string representation of this modifier.
    /// @return The string name of the modifier.
    @Override
    public String toString() {
        return this.name;
    }
}
