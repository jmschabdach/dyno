package edu.drexel.dyno;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.DialogInterface;
import android.os.Bundle;
import android.os.Environment;
import android.text.InputType;
import android.util.Log;
import android.view.View;
import android.widget.*;

import java.io.*;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Set;
import java.util.TreeMap;

public class MyActivity extends Activity {

    public final static String TAG = "DYNO";
    public String filename = "example.txt";
    private static TreeMap<String, OntologyItem> ontology;
    private static final Object ontLock = new Object();
    /**
     * Called when the activity is first created.
     */
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.main);

        ontology = generateOntology(filename);
        // Get the top level to display
        final String[] currentSuperclass = {"base"};
        ArrayList<String> levelItems = getLevelItems(currentSuperclass[0]);

        // Display the current level
        final ArrayAdapter<String> listAdapter = new ArrayAdapter<String>(this, android.R.layout.simple_list_item_1, levelItems);
        final ListView lv = (ListView) findViewById(R.id.list_view);
        lv.setAdapter(listAdapter);
        lv.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                String selected = (String) lv.getItemAtPosition(position);
                Log.d(TAG, selected);
                // How to update list to show the selected item's subclasses?
                if(!selected.equals("No subclasses to show.")) {
                    if (ontology.get(selected).getSubclasses().size() > 0) {
                        Log.d(TAG, "Updated currentSuperclass");
                        ArrayList<String> level = getLevelItems(selected);
                        listAdapter.clear();
                        listAdapter.addAll(level);
                        listAdapter.notifyDataSetChanged();
                    } else {
                        listAdapter.clear();
                        listAdapter.add("No subclasses to show.");
//                    Toast.makeText(getApplicationContext(), "No subclasses to show.", Toast.LENGTH_SHORT).show();
                    }
                    currentSuperclass[0] = selected;
                }
                Log.d(TAG, "Current Superclass: "+currentSuperclass[0]);
            }
        });

        // Make a Dialog for Add button
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        final EditText ed = new EditText(this);
        ed.setInputType(InputType.TYPE_TEXT_FLAG_CAP_SENTENCES);
        builder.setTitle("Add Item")
                .setView(ed)
                .setPositiveButton("Add", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        // get the name
                        String name = ed.getText().toString();
                        // pass it into the new item builder
                        OntologyItem newItem = makeNewItem(name, currentSuperclass[0]);

                        Log.d(TAG, "Name of new item: " + name);
                        Log.d(TAG, "Superclass: " + newItem.getSuperclass().getName());
                        Log.d(TAG, "Level: " + newItem.getLevel());
                        Log.d(TAG, "Prefix: " + newItem.getPrefix());

                        // add new item to ontology
                        ontology.put(name, newItem);
                        // update the item's superclass to reflect the new subclass
                        OntologyItem temp = ontology.get(currentSuperclass[0]);
                        ontology.remove(currentSuperclass[0]);
                        temp.addSubclass(newItem);
                        ontology.put(currentSuperclass[0], temp);

                        // update ontology file
                        saveUpdatedOntology(filename);

                        listAdapter.clear();
                        listAdapter.addAll(getLevelItems(currentSuperclass[0]));
                        listAdapter.notifyDataSetChanged();
                    }
                })
                .setNegativeButton("Cancel", null);
        final AlertDialog addDialog = builder.create();

        // Add listener for "Go Up" button
        // ideally set the behavior for the back button to go up a level too, but you know, time.
        Button up = (Button)findViewById(R.id.go_up);
        up.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (!currentSuperclass[0].equals("base")) {
                    currentSuperclass[0] = ontology.get(currentSuperclass[0]).getSuperclass().getName();
                    ArrayList<String> level = getLevelItems(currentSuperclass[0]);
                    listAdapter.clear();
                    listAdapter.addAll(level);
                    listAdapter.notifyDataSetChanged();
                } else {
                    Toast.makeText(getApplicationContext(), "At top level", Toast.LENGTH_SHORT).show();
                }
            }
        });

        // Add listener for "Add Item" button
        Button add = (Button)findViewById(R.id.add);
        add.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                ed.setText("");
                addDialog.show();
            }
        });
    }

    /**
     * getLevelItems
     * Get an ArrayList<String> of all the names of the items on the current level
     */
    public ArrayList<String> getLevelItems(String superclass){
        ArrayList<OntologyItem> currentLevel = ontology.get(superclass).getSubclasses();
        // Collect the names of the items on the current level
        ArrayList<String> items = new ArrayList<String>();
        for (OntologyItem i : currentLevel){
            items.add(i.getName());
        }
        return items;
    }

    /**
     * generateOntology
     * Reads in .txt file (fn is the name including extension) and generates
     * the ontology TreeMap from it.
     */
    public static TreeMap<String, OntologyItem> generateOntology(String fn){
        // Find file and read it in
        File sdcard = Environment.getExternalStorageDirectory();
        File file = new File(sdcard,"dyno/"+fn);
        StringBuilder text = new StringBuilder();

        try {
            BufferedReader br = new BufferedReader(new FileReader(file));
            String line;
            while ((line = br.readLine()) != null) {
                text.append(line);
                text.append('\n');
            }
            br.close();
        }
        catch (IOException e) {
            Log.e(TAG, "Error reading from ontology file");
        }

        // Extract the ontology from the raw file contents
        String[] contents = text.toString().split("\r\n|\r|\n");
        TreeMap<String, OntologyItem> ontology = new TreeMap<String, OntologyItem>();
        OntologyItem base = new OntologyItem("base");
        ontology.put("base", base);
        OntologyItem superclass = base;
        String prevName = "";
        int prevLevel = 0;
        int level = 0;
        for (int k = 0; k < contents.length; k++){
            level = contents[k].indexOf(':');
            String name = contents[k].substring(level+1);
            OntologyItem item = new OntologyItem(contents[k]);
            // set item level
            item.setLevel(level);
            // set item superclass
            if (level == 1){
                superclass = base;
            } else if (prevLevel == level){
                // don't change the superclass
            } else if (level < prevLevel){
                int goUp = prevLevel - level;
                superclass = ontology.get(prevName).getSuperclass();
                while (goUp > 0){
                    superclass = superclass.getSuperclass();
                    goUp--;
                }
            } else if (level > prevLevel){
                // the current item is a subclass of the previous item
                superclass = ontology.get(prevName);
            }
            item.setSuperclass(superclass);

            // set item as a subclass of the superclass
            superclass.addSubclass(item);
            ontology.put(superclass.getName(), superclass);

            // add item to ontology
            ontology.put(name, item);

            prevName = name;
            prevLevel = level;
        }
        return ontology;
    }

    /**
     * makeNewItem
     * Makes a new OntologyItem to add to the TreeMap ontology using information from
     * current ontology, current superclass, and the name entered by the user
     */
    public static OntologyItem makeNewItem(String name, String superclass) {// add the item to the ontology
        OntologyItem newItem = new OntologyItem();
        newItem.setName(name);
        newItem.setSuperclass(ontology.get(superclass));
        // easily get the level
        int level = ontology.get(superclass).getLevel() + 1;
        newItem.setLevel(level);
        // and now for the hard part...the prefix
        ArrayList<OntologyItem> items = ontology.get(superclass).getSubclasses();
        String highestPrefix = "";
        if (items.size() > 0) {
            for (OntologyItem i : items) {
                if (i.getPrefix().compareTo(highestPrefix) >= 0) {
                    highestPrefix = i.getPrefix();
                }
            }
            System.out.println("Highest Prefix: " + highestPrefix + " out of " + ontology.get(superclass).getSubclasses().size());
            String inc = highestPrefix.substring(highestPrefix.indexOf(':') - 1, highestPrefix.indexOf(':'));
            System.out.println("Attempting to generate prefix: " + inc);
            char n = inc.charAt(0);
            n++;
            newItem.setPrefix(highestPrefix.substring(0, highestPrefix.indexOf(':') - 1) + n + ":");
        } else {
            highestPrefix = ontology.get(superclass).getPrefix();
            String pre = highestPrefix.substring(0, highestPrefix.indexOf(':'));
            newItem.setPrefix(pre + "a:");
        }
        return newItem;
    }

    public static void saveUpdatedOntology(String fn){
        // ontology to String
        Set<String> keys = ontology.keySet();
        String[] contentsOut = new String[keys.size()-1];
        int count = 0;
        for (String key: keys){
            Log.d(TAG, ontology.get(key).toString());
            if(!key.equals("base")) {
                contentsOut[count] = ontology.get(key).toString();
                count++;
            }
        }
        Arrays.sort(contentsOut);

        File sdcard = Environment.getExternalStorageDirectory();
        File file = new File(sdcard,"dyno/"+fn);

        try {
            BufferedWriter bw = new BufferedWriter(new FileWriter(file));
            for (int k = 0; k < contentsOut.length; k++){
                Log.d(TAG, contentsOut[k]);
                bw.write(contentsOut[k]);
                bw.newLine();
            }
            bw.close();
        }
        catch (IOException e) {
            Log.e(TAG, "Error writing to ontology file");
            Log.e(TAG, e.getMessage());
        }
    }
}
