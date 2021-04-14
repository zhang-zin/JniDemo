package com.zj.jnidemo;

public class Person {

    public String name;
    public int age;

    public Person(){

    }

    public Person(String name) {
        this.name = name;
    }

    //()Lstring/lang/String;
    public String getName() {
        return name;
    }

    public void setAge(int age) {
        this.age = age;
    }

    public void setName(String name) {
        this.name = name;
    }

    @Override
    public String toString() {
        return "Person{" +
                "name='" + name + '\'' +
                ", age=" + age +
                '}';
    }
}
