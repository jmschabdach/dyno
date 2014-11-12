package edu.drexel.dyno;

import java.io.File;
import java.util.ArrayList;

/**
 * Created by jenna on 11/6/14.
 */
public class OntologyItem {

    // Properties
    private ArrayList<OntologyItem> subclasses;
    private OntologyItem superclass;
    private String name;
    private int level;
    private String prefix;

    // OntologyItem Constructors
    public OntologyItem(String line){
        this.prefix = line.substring(0, line.indexOf(':')+1);
        this.name = line.substring(line.indexOf(':')+1);
        this.subclasses = new ArrayList<OntologyItem>();
    }

    public OntologyItem(){
        this.subclasses = new ArrayList<OntologyItem>();
    }

    // Getters and Setters
    public void setSubclasses(ArrayList<OntologyItem> sub){
        this.subclasses = sub;
    }

    public ArrayList<OntologyItem> getSubclasses(){
        return this.subclasses;
    }

    public void setSuperclass(OntologyItem sup){
        this.superclass = sup;
    }

    public OntologyItem getSuperclass(){
        if (this.superclass != null) {
            return this.superclass;
        } else {
            return new OntologyItem("");
        }
    }

    public void setName(String n){
        this.name = n;
    }

    public String getName(){
        return this.name;
    }

    public void setLevel(int l){
        this.level = l;
    }

    public int getLevel(){
        return this.level;
    }

    public void setPrefix(String p){
        this.prefix = p;
    }

    public String toString(){
        return this.prefix+this.name;
    }

    public String getPrefix(){
        return this.prefix;
    }
    // Get the ontology/heirarchy from .txt file
    public OntologyItem ontFromFile(File f){
        OntologyItem level = new OntologyItem();
        return level;
    }

    // Add class to subclasses
    public void addSubclass(OntologyItem ontl){
        this.subclasses.add(ontl);
    }
}
