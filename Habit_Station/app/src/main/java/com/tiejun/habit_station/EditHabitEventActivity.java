/*
 * Copyright (c) 2017 Team24, CMPUT301, University of Alberta - All Rights Reserved.
 * You mayuse,distribute, or modify thid code under terms and condition of the Code of Student Behavior at University of Alberta.
 * You can find a copy of the license in this project. Otherwise please contact xuanyi@ualberta.ca.
 *
 */

package com.tiejun.habit_station;

import android.Manifest;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.location.Location;
import android.location.LocationManager;
import android.media.Image;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.Build;
import android.provider.MediaStore;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Base64;
import android.util.Log;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.DatePicker;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;

import org.osmdroid.util.GeoPoint;

import java.io.BufferedReader;
import java.io.ByteArrayOutputStream;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.lang.reflect.Array;
import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.HashSet;
import java.util.StringTokenizer;
import java.util.concurrent.ExecutionException;

/**
 * Activity to add, edit and delete habit events
 *
 *
 * @author xuanyi
 * @version 2.0
 * @since 1.0
 *
 *
 * Use GPS to get location:
 * refer to github BugFree
 * https://github.com/CMPUT301W17T01/BugFree/blob/master/app/src/main/java/com/example/mac/bugfree/activity/CreateEditMoodActivity.java
 *
 * check Image size:
 * refer to github BugFree
 * https://github.com/CMPUT301W17T01/BugFree/blob/master/app/src/main/java/com/example/mac/bugfree/module/Image.java
 *
 *
 */

public class EditHabitEventActivity extends AppCompatActivity {

    private TextView info;
    private EditText comment;
    private DatePicker simpleDatePicker;
    private Button image;
    private Bitmap photo = null;
    //    private Bitmap oldPhoto = null;
    private String oldPhoto = null;
    // new
    private GeoPoint currentLocation;
    private static final int REQUEST_CODE_ASK_PERMISSIONS = 123;

    private byte[] imageByteArray;
    private String imageBase64;
    private int imageByteCount;

    //
    private static final String FILENAME2 = "habitLibrary.sav";// for save and load
    private String FILENAME1;


    protected HabitEventList habitEventList = new HabitEventList();
    protected HabitEvent habitEvent;
    private String habit_name;
    private Habit habit;

    private ArrayList<HabitEvent> fillist = new ArrayList<HabitEvent>();

    //
    private ArrayList<Habit> habits = new ArrayList<Habit>();

    //
    public int do_year = 0,
            do_month = 0,
            do_day = 0;

    static final int REQUEST_IMAGE_CAPTURE = 1;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_edit_habit_event);

        SharedPreferences pref = getSharedPreferences("data", MODE_PRIVATE);
        final String userName = pref.getString("currentUser", "");
        Intent intent = getIntent();
        //final int habitIndex = intent.getIntExtra("habit index", 0);

        final int eventIndex = intent.getIntExtra("select", 0);
        habit_name = intent.getStringExtra("habit name");

        comment = (EditText) findViewById(R.id.comment);
        simpleDatePicker = (DatePicker) findViewById(R.id.datePicker);


        String habit_id = userName + habit_name.toUpperCase();
        Log.d("habitid", habit_id);
        /**
         * a offline behaviour handler code
         * use local buffer to show
         */
        if( isNetworkAvailable(this) == false){

            Toast.makeText(getApplicationContext(), "You are now in offline mode.", Toast.LENGTH_SHORT).show();

            loadFromFile(FILENAME2);

            for (Habit element: habits){
                String id = element.getuName()+element.getTitle().toUpperCase();
                if (id.equals(habit_id)){
                    habit = element;
                }
            }

        }
        else {

            ElasticSearchHabitController.GetHabitTask getHabit
                    = new ElasticSearchHabitController.GetHabitTask();
            getHabit.execute(habit_id);

            try {
                habit = getHabit.get();  //other way later
            } catch (InterruptedException e) {
                e.printStackTrace();
                Log.d("habitinfo", "???");
            } catch (ExecutionException e) {
                e.printStackTrace();
                Log.d("habitinfo", "???");
            }
        }

        HashSet<Integer> days = habit.getRepeatWeekOfDay();
        ArrayList<String> sdays = getPlans(days);


        final Calendar startDate = habit.getStartDate();
        final HashSet<Integer> weekDay = habit.getRepeatWeekOfDay();


        final Calendar doDate = Calendar.getInstance();
        if (do_year == 0 && do_month == 0 && do_day == 0) {
            //
            do_year = doDate.get(Calendar.YEAR);
            do_month = doDate.get(Calendar.MONTH);
            do_day = doDate.get(Calendar.DAY_OF_MONTH);
        }

        //new
        final Button locationBtn = (Button) findViewById(R.id.location);                        //  click the button to save the information
        locationBtn.setOnClickListener(new View.OnClickListener() {

            public void onClick(View v) {
                setResult(RESULT_OK);

                // URL : http://developer.android.com/guide/topics/ui/dialogs.html
                AlertDialog.Builder builder1 = new AlertDialog.Builder(EditHabitEventActivity.this);
                builder1.setTitle("Add a location")
                        .setMessage("Add current location?")
                        .setPositiveButton("Yes", new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialog, int which) {
                                Log.d("location", "current");
                                add_location();
                                if(currentLocation != null) {
                                    Toast.makeText(getApplicationContext(), currentLocation.toString(), Toast.LENGTH_LONG).show();
                                }
                            }
                        });
                builder1.show();
            }
        });


        //


        final Button confirmBtn = (Button) findViewById(R.id.save);                        //  click the button to save the information
        confirmBtn.setOnClickListener(new View.OnClickListener() {

            public void onClick(View v) {
                setResult(RESULT_OK);
                boolean added = true;
                String sComment = comment.getText().toString();


                doDate.set(do_year, do_month, do_day);                                      //time comparison
                if ((startDate.get(Calendar.YEAR) == doDate.get(Calendar.YEAR))
                        && ((startDate.get(Calendar.MONTH) == doDate.get(Calendar.MONTH)))
                        && ((startDate.get(Calendar.DAY_OF_MONTH) == doDate.get(Calendar.DAY_OF_MONTH)))) {

                    added = true;
                } else {
                    if (startDate.after(doDate)) {
                        Toast.makeText(getApplicationContext(), "Should after startDate.", Toast.LENGTH_SHORT).show();
                        added = false;
                    }
                }

                if (sComment.length() > 20) {
                    comment.setError("Title should not be empty and should be at most 20 words");
                    added = false;
                }


                Calendar today = Calendar.getInstance();
                if (doDate.after(today)) {
                    Toast.makeText(getApplicationContext(), "Are you sure you have done this event?", Toast.LENGTH_SHORT).show();
                    added = false;
                }


                Log.d("TTT", "Week day is" + String.valueOf(doDate.get(Calendar.DAY_OF_WEEK) - 1));
                int day_of_week = doDate.get(Calendar.DAY_OF_WEEK) - 1;
                if (!weekDay.contains(day_of_week)) {
                    Toast.makeText(getApplicationContext(), "Not in the plan.", Toast.LENGTH_SHORT).show();
                    added = false;
                }


                if (added) {


                    HabitEvent event = new HabitEvent(userName, habit.getTitle(), doDate, sComment);
                    if (currentLocation != null) {
                        event.seteLocation(currentLocation);
                    }
                    if (photo != null) {
                        String base = checkImageSize(photo);
                        event.setePhoto(base);
                    } else {
                        //photo = oldPhoto;
                        event.setePhoto(oldPhoto);
                    }


                    added = setEvent(userName, event, eventIndex);


                }
                if (added) {

                   /* Intent intent = new Intent(EditHabitEventActivity.this, HabitEventLibraryActivity.class);
                   // intent.putExtra("habit index", habitIndex);   // later will be useless
                    intent.putExtra("habit name",habit_name);
                    startActivity(intent);
                    */

                }


            }
        });

        Calendar calendar = Calendar.getInstance();
        calendar.setTimeInMillis(System.currentTimeMillis());
        simpleDatePicker.init(calendar.get(Calendar.YEAR), calendar.get(Calendar.MONTH),
                calendar.get(Calendar.DAY_OF_MONTH), new DatePicker.OnDateChangedListener() {
                    @Override
                    public void onDateChanged(DatePicker datePicker, int year, int month, int dayOfMonth) {

                        do_year = simpleDatePicker.getYear();
                        do_month = simpleDatePicker.getMonth();
                        do_day = simpleDatePicker.getDayOfMonth();
                    }
                });
    }

    @Override
    protected void onStart() {
        // TODO Auto-generated method stub
        super.onStart();
        // new
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            permissionLocationRequest();
            Log.d("gps", "ask for permission");

        } else {
            Log.d("gps", "allowed");
            Toast.makeText(this, "Allowed to use GPS!", Toast.LENGTH_SHORT).show();
        }
        //

        SharedPreferences pref = getSharedPreferences("data", MODE_PRIVATE);
        String userName = pref.getString("currentUser", "");
        Intent intent = getIntent();
        int eventIndex = intent.getIntExtra("select", 0);
        habit_name = intent.getStringExtra("habit name");
        String event_query = intent.getStringExtra("query");


        info = (TextView) findViewById(R.id.info);
        String habit_id = userName + habit_name.toUpperCase();
        //Habit habit = new Habit();
        /**
         * a offline behaviour handler code
         * use local buffer to show
         */
        if( isNetworkAvailable(this) == false){

            Toast.makeText(getApplicationContext(), "You are now in offline mode.", Toast.LENGTH_SHORT).show();
            habits.clear();
            loadFromFile(FILENAME2);

            for (Habit element: habits){
                String id = element.getuName()+element.getTitle().toUpperCase();
                if (id.equals(habit_id)){
                    habit = element;
                }
            }

        }

        else {  // start of online block
            ElasticSearchHabitController.GetHabitTask getHabit
                    = new ElasticSearchHabitController.GetHabitTask();
            getHabit.execute(habit_id);

            try {
                habit = getHabit.get();
            } catch (InterruptedException e) {
                e.printStackTrace();
            } catch (ExecutionException e) {
                e.printStackTrace();
            }
        }
    ///

        HashSet<Integer> days = habit.getRepeatWeekOfDay();
        ArrayList<String> sdays = getPlans(days);

        info.setText(habit.toString() + "\nReason: " + habit.getReason() + "\nPlan: " + sdays);


        if (eventIndex >= 0) {    // if the event already exists, show its old info         edit, need info directly from event
//
            HabitEvent event;
            if( isNetworkAvailable(this) == false){
                FILENAME1 = userName + habit_name +".sav";
                loadFromFile(FILENAME1);
                event = fillist.get(eventIndex);
            }
            else {
                ElasticSearchEventController.GetEvents getEvents
                        = new ElasticSearchEventController.GetEvents();
                getEvents.execute(event_query);
                try {
                    fillist.clear();
                    fillist.addAll(getEvents.get());
                } catch (InterruptedException e) {
                    e.printStackTrace();
                } catch (ExecutionException e) {
                    e.printStackTrace();
                }

                event = fillist.get(eventIndex);


            }
            //
            comment = (EditText) findViewById(R.id.comment);
            comment.setText(event.geteComment());

            simpleDatePicker = (DatePicker) findViewById(R.id.datePicker);
            simpleDatePicker.updateDate(event.geteTime().get(Calendar.YEAR), event.geteTime().get(Calendar.MONTH), event.geteTime().get(Calendar.DAY_OF_MONTH));

            // new
            currentLocation = event.geteLocation();
            oldPhoto = event.getePhoto();
            //


        } else {
            Log.d("TTT", "event haven't been created");
        }


    }

    /**
     * Set a habit event to its corresponding user
     *
     * @param current_user username
     * @param new_event habit event
     * @param eventIndex event index
     * @return
     */
    public boolean setEvent(String current_user, HabitEvent new_event, int eventIndex) {

        FILENAME1 = current_user+habit_name +".sav";

        String new_id = current_user + new_event.geteName()
                + new_event.geteTime().get(Calendar.YEAR)
                + String.valueOf(new_event.geteTime().get(Calendar.MONTH) + 1)
                + new_event.geteTime().get(Calendar.DAY_OF_MONTH);


        if (eventIndex >= 0) {              //  used for edit

            Intent intent = getIntent();
            String event_query = intent.getStringExtra("query");

            HabitEvent old_event;
            if( isNetworkAvailable(this) == false){
                loadFromFile(FILENAME1);
                old_event = fillist.get(eventIndex);
            }
            else {
                ElasticSearchEventController.GetEvents getEvents
                        = new ElasticSearchEventController.GetEvents();
                getEvents.execute(event_query);
                try {
                    fillist.clear();
                    fillist.addAll(getEvents.get());
                } catch (InterruptedException e) {
                    e.printStackTrace();
                } catch (ExecutionException e) {
                    e.printStackTrace();
                }

                old_event = fillist.get(eventIndex);
            }
            String old_id = current_user + old_event.geteName()
                    + old_event.geteTime().get(Calendar.YEAR)
                    + String.valueOf(old_event.geteTime().get(Calendar.MONTH) + 1)
                    + old_event.geteTime().get(Calendar.DAY_OF_MONTH);


            if ((existedEvent(new_id)) && !(new_id.equals(old_id))) {
                Toast.makeText(getApplicationContext(), "This event for today already exists !", Toast.LENGTH_SHORT).show();
                return false;
            } else {

                if( isNetworkAvailable(this) == false) {
                    fillist.remove(eventIndex);
                    fillist.add(new_event);
                    saveInFile();
                }
                else {         // online
                    ///////  delete old event /////
                    ElasticSearchEventController.DeleteEventTask deleteEventTask
                            = new ElasticSearchEventController.DeleteEventTask();
                    deleteEventTask.execute(old_event);

                    ////// add new event ///////
                    ElasticSearchEventController.AddEventTask addEventTask
                            = new ElasticSearchEventController.AddEventTask();
                    addEventTask.execute(new_event);
                    fillist.remove(eventIndex);
                    fillist.add(new_event);
                    saveInFile();
                }
                Toast.makeText(getApplicationContext(), "Successfully updated the event.", Toast.LENGTH_SHORT).show();

                return true;
            }

        } else {         //  used for add
            ////// add new event ///////
            if (existedEvent(new_id)) {
                Toast.makeText(getApplicationContext(), "This event for today already exists !", Toast.LENGTH_SHORT).show();
                return false;
            }

            if( isNetworkAvailable(this) == false){
                loadFromFile(FILENAME1);
                fillist.add(new_event);
                saveInFile();
                Toast.makeText(getApplicationContext(), "Successfully added to history.", Toast.LENGTH_SHORT).show();
                return true;

            }
            else{  // online
                ElasticSearchEventController.AddEventTask addEventTask
                        = new ElasticSearchEventController.AddEventTask();
                addEventTask.execute(new_event);
                saveInFile();
                Toast.makeText(getApplicationContext(), "Successfully added to history.", Toast.LENGTH_SHORT).show();
                return true;
            }

        }

    }

    /**
     * Get the plan of the habit
     *
     * @param days occurrence weekday
     * @return
     */
    public ArrayList<String> getPlans(HashSet<Integer> days) {
        ArrayList<String> sdays = new ArrayList<String>();
        if (days.contains(1)) {
            sdays.add("M");
        }
        if (days.contains(2)) {
            sdays.add("T");
        }
        if (days.contains(3)) {
            sdays.add("W");
        }
        if (days.contains(4)) {
            sdays.add("R");
        }
        if (days.contains(5)) {
            sdays.add("F");
        }
        if (days.contains(6)) {
            sdays.add("SAT");
        }
        if (days.contains(0)) {
            sdays.add("SUN");
        }

        return sdays;

    }

    /**
     * Check if the event is already existed
     *
     * @param id habit event ID
     * @return
     */
    private boolean existedEvent(String id) {
        if( isNetworkAvailable(this) == false){
            SharedPreferences pref = getSharedPreferences("data", MODE_PRIVATE);
            String userName = pref.getString("currentUser", "");
            FILENAME1 = userName + habit_name +".sav";
            loadFromFile(FILENAME1);
            for (HabitEvent element: fillist){
                String eid =element.getuName()+element.geteName()
                        +element.geteTime().get(Calendar.YEAR)
                        +String.valueOf(element.geteTime().get(Calendar.MONTH)+1)
                        +element.geteTime().get(Calendar.DAY_OF_MONTH);
                if (eid.equals(id)){
                    return true;
                }

            }
            return false;
        }
        else {
            ElasticSearchEventController.IsExist isExist = new ElasticSearchEventController.IsExist();
            isExist.execute(id);
            try {
                if (isExist.get()) {
                    return true;
                } else {
                    return false;
                }
            } catch (Exception e) {
                return false;
            }
        }

    }

    /**
     * Launch the camera
     *
     * @param view Current view
     */
    public void takePhoto(View view) {
        Intent intent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
        // take a picture and pass result along to onActivityResult
        startActivityForResult(intent, REQUEST_IMAGE_CAPTURE);

    }

    // RETURN THE IMAGE
    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (requestCode == REQUEST_IMAGE_CAPTURE && resultCode == RESULT_OK) {
            // GET THE PHOTO
            Bundle extras = data.getExtras();
            photo = (Bitmap) extras.get("data");
            //habitEvent.setePhoto(photo);
            Intent intent = new Intent(EditHabitEventActivity.this, PhotoDisplayActivity.class);
            intent.putExtra("image", photo);
            startActivity(intent);

        }
    }

    /**
     * Check the if image size is greater than 65536
     *
     * @param bm photo
     * @return
     */
    private String checkImageSize(Bitmap bm) {
        imageByteArray = changeImageIntoByteArray(bm);
        imageByteCount = imageByteArray.length;
        String base64;
        if (imageByteCount >= 65536) {
            resizeImage(bm);
            base64 = checkImageSize(bm);
        } else {
            base64 = imageToBase64(bm);
        }
        return base64;
    }

    /**
     * Convert bitmap object to a byte array
     *
     * @param bitmap photo
     * @return
     */
    private byte[] changeImageIntoByteArray(Bitmap bitmap) {
        ByteArrayOutputStream stream = new ByteArrayOutputStream();
        bitmap.compress(Bitmap.CompressFormat.JPEG, 100, stream);
        return stream.toByteArray();
    }

    /**
     * Resize the photo
     *
     * @param bitmap photo
     */
    private void resizeImage(Bitmap bitmap) {
        ByteArrayOutputStream stream = new ByteArrayOutputStream();
        bitmap.compress(Bitmap.CompressFormat.JPEG, 70, stream);
    }

    /**
     * Convert bitmap object to base64 string
     * Used to save into the elasticsearch
     *
     * @param bm photo
     * @return
     */
    private String imageToBase64(Bitmap bm) {
        imageBase64 = Base64.encodeToString(changeImageIntoByteArray(bm), Base64.NO_WRAP);
        return imageBase64;
    }

    /**
     * Convert base64 to an bitmap object
     * Used to display photo on the screen
     *
     * @return
     */
    public Bitmap base64ToImage() {
        byte[] decodedString = Base64.decode(imageBase64, Base64.DEFAULT);
        Bitmap decodedByte = BitmapFactory.decodeByteArray(decodedString, 0,
                decodedString.length);

        return decodedByte;
    }


    /**
     * Grand the GPS permission
     * https://github.com/CMPUT301W17T01/BugFree/blob/master/app/src/main/java/com/example/mac/bugfree/activity/CreateEditMoodActivity.java
     */
    private void permissionLocationRequest() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            int hasLocationPermission = checkSelfPermission(Manifest.permission.ACCESS_FINE_LOCATION);
            if (hasLocationPermission != PackageManager.PERMISSION_GRANTED) {
                if (!shouldShowRequestPermissionRationale(Manifest.permission.ACCESS_FINE_LOCATION)) {
                    showMessageOKCancel("You need to allow access to Location",
                            new DialogInterface.OnClickListener() {
                                @Override
                                public void onClick(DialogInterface dialog, int which) {
                                    requestPermissions(new String[]{Manifest.permission.ACCESS_FINE_LOCATION},
                                            REQUEST_CODE_ASK_PERMISSIONS);
                                }
                            });
                }
            }

        }
    }

    /**
     * Show message alert
     *
     * @param message Message
     * @param okListener listener to the ok button
     */
    private void showMessageOKCancel(String message, DialogInterface.OnClickListener okListener) {
        new AlertDialog.Builder(EditHabitEventActivity.this)
                .setMessage(message)
                .setPositiveButton("OK", okListener)
                .setNegativeButton("Cancel", null)
                .create()
                .show();
    }

    /**
     * Using GPS to add current location.
     * https://github.com/CMPUT301W17T01/BugFree/blob/master/app/src/main/java/com/example/mac/bugfree/activity/CreateEditMoodActivity.java
     */
    public void add_location() {
        try {
            CurrentLocation locationListener = new CurrentLocation();
            LocationManager locationManager = (LocationManager) getSystemService(Context.LOCATION_SERVICE);
            locationManager.requestLocationUpdates(LocationManager.GPS_PROVIDER, 0, 0, locationListener);
            Location location = locationManager.getLastKnownLocation(LocationManager.GPS_PROVIDER);
            if (location != null) {
                int latitude = (int) (location.getLatitude() * 1E6);
                int longitude = (int) (location.getLongitude() * 1E6);
                currentLocation = new GeoPoint(latitude, longitude);
            }
            else{
                currentLocation = null;
                Toast.makeText(this, "GPS not ready.", Toast.LENGTH_SHORT).show();
            }
        } catch (SecurityException e) {
            e.printStackTrace();
        }

    }




    /**
     * a offline detecter
     * Source: https://stackoverflow.com/questions/4238921/detect-whether-there-is-an-internet-connection-available-on-android
     * @param c
     * @return
     */
    private boolean isNetworkAvailable(Context c) {
        ConnectivityManager connectivityManager = (ConnectivityManager) c.getSystemService(Context.CONNECTIVITY_SERVICE);
        NetworkInfo activeNetworkInfo = connectivityManager.getActiveNetworkInfo();
        return activeNetworkInfo != null && activeNetworkInfo.isConnected();
    }

    /**
     * a method to save in file
     * source: https://github.com/wooloba/lonelyTwitter/blob/master/app/src/main/java/ca/ualberta/cs/lonelytwitter/LonelyTwitterActivity.java
     * from old lab exercise
     */
    private void saveInFile() {
        try {
            FileOutputStream fos = openFileOutput(FILENAME1,
                    Context.MODE_PRIVATE);
            OutputStreamWriter writer = new OutputStreamWriter(fos);
            Gson gson = new Gson();
            gson.toJson(fillist, writer);
            writer.flush();
            fos.close();
        } catch (FileNotFoundException e) {
            // TODO Auto-generated catch block
            throw new RuntimeException();

            //e.printStackTrace();
        } catch (IOException e) {
            // TODO Auto-generated catch block
            throw new RuntimeException();
            //e.printStackTrace();
        }

        Log.d("Error","done save in file");
    }

    /**
     * a method to load from file
     * source: https://github.com/wooloba/lonelyTwitter/blob/master/app/src/main/java/ca/ualberta/cs/lonelytwitter/LonelyTwitterActivity.java
     * from old lab excercise
     */
    private void loadFromFile(String FILENAME) {
        //ArrayList<String> tweets = new ArrayList<String>();
        try {
            FileInputStream fis = openFileInput(FILENAME);
            BufferedReader in = new BufferedReader(new InputStreamReader(fis));
            Gson gson = new Gson();
            if (FILENAME.equals(FILENAME1)){                  // load from event list
                Type listType = new TypeToken<ArrayList<HabitEvent>>(){}.getType();
                fillist = gson.fromJson(in, listType);
            }
            else{
                Type listType = new TypeToken<ArrayList<Habit>>(){}.getType();
                habits = gson.fromJson(in, listType);
            }

        } catch (FileNotFoundException e) {
            //TODO Auto-generated catch block
            if (FILENAME.equals(FILENAME1)) {
                fillist = new ArrayList<HabitEvent>();
            }
            else{
                habits = new ArrayList<Habit>();

            }
            e.printStackTrace();
        } catch (IOException e) {
            // TODO Auto-generated catch block
            throw new RuntimeException(e);
            //e.printStackTrace();
        }

    }





}
