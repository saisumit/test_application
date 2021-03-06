package com.example.saisumit.test_application;

import android.app.DownloadManager;
import android.content.ActivityNotFoundException;
import android.content.Intent;
import android.speech.RecognizerIntent;
import android.speech.tts.TextToSpeech;
import android.support.annotation.NonNull;
import android.support.design.widget.FloatingActionButton;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.text.format.DateFormat;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.EditText;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import com.firebase.client.Firebase;
import com.firebase.ui.FirebaseListAdapter;
import com.firebase.ui.auth.AuthUI;
import com.firebase.client.Query;

import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import java.util.ArrayList;
import java.util.Locale;


public class MainActivity extends AppCompatActivity {

    TextToSpeech t1;
    String ed1;

    public static int SIGN_IN_REQUEST_CODE=101;
    public static int SPEECH_RECOGNITION_CODE=102;
    public static String TAG="MainActivity";
    private FirebaseListAdapter<ChatMessage> adapter;
    private EditText mInputBox;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        Firebase.setAndroidContext(this);
        if(FirebaseAuth.getInstance().getCurrentUser() == null) {
            // Start sign in/sign up activity
            startActivityForResult(
                    AuthUI.getInstance()
                            .createSignInIntentBuilder()
                            .build(),
                    SIGN_IN_REQUEST_CODE
            );
        } else {
            // User is already signed in. Therefore, display
            // a welcome Toast
            Toast.makeText(this, "Welcome " + FirebaseAuth.getInstance()
                            .getCurrentUser()
                            .getDisplayName(), Toast.LENGTH_LONG).show();

            // Load chat room contents
            displayChatMessages();
        }

        mInputBox = findViewById(R.id.input);
        FloatingActionButton fab = findViewById(R.id.fab);

        fab.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {

                // Read the input field and push a new instance
                // of ChatMessage to the Firebase database
                FirebaseDatabase.getInstance()
                        .getReference()
                        .push()
                        .setValue(new ChatMessage(mInputBox.getText().toString(),
                                        FirebaseAuth.getInstance()
                                                .getCurrentUser()
                                                .getDisplayName())
                        );

                // Clear the input
                mInputBox.setText("");
            }
        });



        // Experiment
        FloatingActionButton b1 =  (FloatingActionButton)findViewById(R.id.speak);
       // ed1.setText("Hey Sumit miss u a lot");
        t1 = new TextToSpeech(getApplicationContext(), new TextToSpeech.OnInitListener() {
            @Override
            public void onInit(int status) {
                if(status != TextToSpeech.ERROR) {
                    t1.setLanguage(Locale.UK);
                }
            }
        });

        b1.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {

                DatabaseReference databaseReference = FirebaseDatabase.getInstance().getReferenceFromUrl("https://chatapplication-8a107.firebaseio.com/") ;
                com.google.firebase.database.Query lastQuery = databaseReference.limitToLast(1);

                lastQuery.addListenerForSingleValueEvent(new ValueEventListener() {
                    @Override
                    public void onDataChange(DataSnapshot dataSnapshot) {
                        String message = dataSnapshot.getValue().toString();
                        Log.e("message + ", message) ;
                        String[] messagearray = message.split("messageText=") ;
                        Log.e("message array", messagearray[0]) ;
                        String[] toSpeak = messagearray[1].split(", messageTime");
                        String[] messageUser = toSpeak[0].split("messageUser");
                        String finalspeak = "Latest message " + toSpeak[0].substring(0,toSpeak[0].length()-2) ;
                        Log.e("message user",messageUser[0]);
                        Toast.makeText(getApplicationContext(), finalspeak, Toast.LENGTH_SHORT).show();
                        t1.speak(finalspeak, TextToSpeech.QUEUE_FLUSH, null);
                    }

                    @Override
                    public void onCancelled(DatabaseError databaseError) {
                        //Handle possible errors.
                    }
            });
            }
        });

        // !Experiment

        FloatingActionButton mic_button =  findViewById(R.id.mic);
        mic_button.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                startSpeechToText();
            }
        });

    }

    public void onPause(){
        if(t1 !=null){
            t1.stop();
            t1.shutdown();
        }
        super.onPause();
    }

    private void displayChatMessages() {

        Log.i(TAG, "displayChatMessages");
        ListView listOfMessages = findViewById(R.id.list_of_messages);

        Firebase ref = new Firebase("https://chatapplication-8a107.firebaseio.com/");

        adapter = new FirebaseListAdapter<ChatMessage>(this,ChatMessage.class,
                R.layout.message,ref ){

                @Override
                protected void populateView(View v, ChatMessage model, int position) {
                // Get references to the views of message.xml
//                // Experiment
                    TextView theFact = (TextView) v.findViewById(R.id.message_text);
                    String shareFact = theFact.getText().toString();
                    Log.e("Lets read the messages " , shareFact + " " + position ) ;
                    ed1 = shareFact  ;

//                // !Experiment

                TextView messageText = v.findViewById(R.id.message_text);
                TextView messageUser = v.findViewById(R.id.message_user);
                TextView messageTime = v.findViewById(R.id.message_time);

                // Set their text
                messageText.setText(model.getMessageText());
                messageUser.setText(model.getMessageUser());

                // Format the date before showing it
                messageTime.setText(DateFormat.format("dd-MM-yyyy (HH:mm:ss)",
                        model.getMessageTime()));
                }
            };

        listOfMessages.setAdapter(adapter);
    }


    private void startSpeechToText() {

        Intent intent = new Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH);
        intent.putExtra(RecognizerIntent.EXTRA_LANGUAGE, Locale.getDefault());
        intent.putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL,
                RecognizerIntent.LANGUAGE_MODEL_FREE_FORM);
        intent.putExtra(RecognizerIntent.EXTRA_PROMPT,
                "Speak something...");
        try {
            startActivityForResult(intent, SPEECH_RECOGNITION_CODE);
        } catch (ActivityNotFoundException a) {
            Toast.makeText(getApplicationContext(),
                    "Sorry! Speech recognition is not supported in this device.",
                    Toast.LENGTH_SHORT).show();
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode,
                                    Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if(requestCode == SIGN_IN_REQUEST_CODE) {
            if(resultCode == RESULT_OK) {
                Toast.makeText(this,"Successfully signed in. Welcome!", Toast.LENGTH_LONG).show();
                displayChatMessages();
            } else {
                Toast.makeText(this,"We couldn't sign you in. Please try again later.", Toast.LENGTH_LONG).show();
                // Close the app
                finish();
            }
        }
        else if(requestCode == SPEECH_RECOGNITION_CODE){
            if (resultCode == RESULT_OK && null != data) {
                ArrayList<String> result = data
                        .getStringArrayListExtra(RecognizerIntent.EXTRA_RESULTS);
                String text = result.get(0);
                mInputBox.setText(text);
            }
        }

    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.main_menu, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if(item.getItemId() == R.id.menu_sign_out) {
            AuthUI.getInstance().signOut(this)
                    .addOnCompleteListener(new OnCompleteListener<Void>() {
                        @Override
                        public void onComplete(@NonNull Task<Void> task) {
                            Toast.makeText(MainActivity.this, "You have been signed out.", Toast.LENGTH_LONG).show();
                            // Close activity
                            finish();
                        }
                    });
        }
        else if(item.getItemId() == R.id.events){
            Intent intent = new Intent(this, EventsActivity.class);
            startActivity(intent);
        }
        return true;
    }

}
