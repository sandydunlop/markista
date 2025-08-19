package io.github.sandydunlop.markista.model;

import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.Serializable;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;

class PairTests {
    Pair<String,String> pair;

    @BeforeEach
    void setup() {
        pair = Pair.of("one","two");
    }

    @Test 
    void whenSerializingAndDeserializing_ThenObjectIsTheSame() throws IOException, ClassNotFoundException { 
        FileOutputStream fileOutputStream = new FileOutputStream("build/yourfile.txt");
        ObjectOutputStream objectOutputStream = new ObjectOutputStream(fileOutputStream);
        objectOutputStream.writeObject(pair);
        objectOutputStream.flush();
        objectOutputStream.close();
        
        FileInputStream fileInputStream = new FileInputStream("build/yourfile.txt");
        ObjectInputStream objectInputStream = new ObjectInputStream(fileInputStream);
        Pair<String, String> p2 = safeCast(objectInputStream.readObject());
        objectInputStream.close(); 
    
        assertEquals(p2.getL(), pair.getL());
        assertEquals(p2.getR(), pair.getR());
    }

    @SuppressWarnings("unchecked")
    private <L extends Serializable, R extends Serializable> Pair<L, R> safeCast(Object obj) {
        return (Pair<L, R>) obj;
    }

    @Test
    void setAndGet() {
        pair.setL("left");
        pair.setR("right");
        assertEquals("left", pair.getL());
        assertEquals("right", pair.getR());
    }

    @Test
    void test_toString() {
        assertEquals("Pair[one,two]", pair.toString());
    }

    @Test
    void test_hashCode() {
        pair.setL(null);
        pair.setR(null);
        assertEquals(0, pair.hashCode());

        String right = "right";
        pair.setR(right);
        assertEquals(right.hashCode() + 1, pair.hashCode());

        String left = "left";
        pair.setL(left);
        pair.setR(null);
        assertEquals(left.hashCode() + 2, pair.hashCode());

        pair.setR(right);
        int expected = left.hashCode() * 17 + right.hashCode();
        assertEquals(expected, pair.hashCode());
    }

    @Test
    void test_equals() {
        Pair<String,String> p = Pair.of(pair.getL(), pair.getR());
        assertEquals(pair, p);
    }

    @Test
    void test_equals_notEqual_0() {
        Pair<Integer,Integer> p = Pair.of(0, 1);
        assertNotEquals(pair, p);
    }

    @Test
    void test_equals_notEqual_1() {
        Pair<String,String> p = Pair.of("different", pair.getR());
        assertNotEquals(pair, p);
    }

    @Test
    void test_equals_notEqual_2() {
        Pair<String,String> p = Pair.of(pair.getL(), "different");
        assertNotEquals(pair, p);
    }

    @Test
    void test_equals_notEqual_3() {
        Pair<String,String> p = Pair.of("notsame", "different");
        assertNotEquals(pair, p);
    }

    @Test
    void test_equals_notEqual_4() {
        String s = "notapair";
        assertNotEquals(pair, s);
    }
}
