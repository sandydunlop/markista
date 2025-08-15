package io.github.sandydunlop.markista.model;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;

import org.junit.jupiter.api.Test;

class PairTests {
    @Test 
    void whenSerializingAndDeserializing_ThenObjectIsTheSame() throws IOException, ClassNotFoundException { 
        Pair<String,String> pair = Pair.of("One","Two");
        
        FileOutputStream fileOutputStream = new FileOutputStream("yourfile.txt");
        ObjectOutputStream objectOutputStream = new ObjectOutputStream(fileOutputStream);
        objectOutputStream.writeObject(pair);
        objectOutputStream.flush();
        objectOutputStream.close();
        
        FileInputStream fileInputStream = new FileInputStream("yourfile.txt");
        ObjectInputStream objectInputStream = new ObjectInputStream(fileInputStream);
        Pair<String, String> p2 = (Pair<String, String>) objectInputStream.readObject();
        objectInputStream.close(); 
    
        assertEquals(p2.getL(), pair.getL());
        assertEquals(p2.getR(), pair.getR());
    }
}
