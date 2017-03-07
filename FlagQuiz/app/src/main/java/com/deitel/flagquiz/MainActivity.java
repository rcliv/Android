// MainActivity.java
// Hosts the MainActivityFragment on a phone and both the
// MainActivityFragment and SettingsActivityFragment on a tablet
package com.deitel.flagquiz;

import android.app.Fragment;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.SharedPreferences.OnSharedPreferenceChangeListener;
import android.content.pm.ActivityInfo;
import android.content.res.Configuration;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.support.v4.app.FragmentTransaction;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.Toast;

import java.util.Set;

public class MainActivity extends AppCompatActivity implements MainActivityFragment.DynamicFrag {
   // keys for reading data from SharedPreferences
   public static final String CHOICES = "pref_numberOfChoices";
   public static final String REGIONS = "pref_regionsToInclude";
   public static final String GUESSES = "pref_numberOfGuesses";
   public static final String QUESTION_NUM = "pref_questionNumber";
   public static final String QUIZ_COUNTRIES = "pref_countriesInQuiz";
   public static final String CORRECT_GUESSES = "pref_numberOfCorrectGuesses";
   public static final String TOTAL_GUESSES = "pref_numberOfTotalGuesses";
   public static final String GUESSES_LEFT = "pref_numberOfGuessesLeftInTurn";
   public static final String INITIAL_START = "pref_initialStart";
   public static final String FILE_NAME_LIST = "pref_fileNameList";
   public static final String CORRECT_ANSWER = "pref_correctAnswer";
   public static final String BUTTON_OPTIONS_LIST = "pref_buttonOptionsList";


   private boolean phoneDevice = true; // used to force portrait mode
   private boolean preferencesChanged = true; // did preferences change?
   private boolean threePrefChanged = false;
   public MainActivityFragment quizFragment;


   // configure the MainActivity
   @Override
   protected void onCreate(Bundle savedInstanceState) {
      super.onCreate(savedInstanceState);
      setContentView(R.layout.activity_main);
      Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
      setSupportActionBar(toolbar);

      // set default values in the app's SharedPreferences
      PreferenceManager.setDefaultValues(this, R.xml.preferences, false);

      // register listener for SharedPreferences changes
      PreferenceManager.getDefaultSharedPreferences(this).
         registerOnSharedPreferenceChangeListener(
            preferencesChangeListener);

      // determine screen size
      int screenSize = getResources().getConfiguration().screenLayout &
         Configuration.SCREENLAYOUT_SIZE_MASK;

      // if device is a tablet, set phoneDevice to false
      if (screenSize == Configuration.SCREENLAYOUT_SIZE_LARGE ||
         screenSize == Configuration.SCREENLAYOUT_SIZE_XLARGE)
         phoneDevice = false; // not a phone-sized device

      // if running on phone-sized device, allow only portrait orientation
      if (phoneDevice)
         setRequestedOrientation(
            ActivityInfo.SCREEN_ORIENTATION_PORTRAIT);
   }

   // called after onCreate completes execution
   @Override
   protected void onStart() {
      super.onStart();

      if (preferencesChanged) {
         // now that the default preferences have been set,
         // initialize MainActivityFragment and start the quiz
//         MainActivityFragment quizFragment = (MainActivityFragment)
//            getSupportFragmentManager().findFragmentById(
//               R.id.quizFragment);

         // Create an instance of ExampleFragment
         if (quizFragment == null) {
            quizFragment = new MainActivityFragment();

            // Add the fragment to the 'fragment_container' FrameLayout
            FragmentTransaction transaction = getSupportFragmentManager().beginTransaction();
            transaction.add(R.id.quizFragment, quizFragment);
            transaction.commit();
            getSupportFragmentManager().executePendingTransactions();
         }

         SharedPreferences sharedPreferences = PreferenceManager.getDefaultSharedPreferences(this);

         quizFragment.setInitialLoadTrue();
         quizFragment.updateGuessRows(sharedPreferences);
         quizFragment.updateRegions(sharedPreferences);
         quizFragment.updateGuessAmount(sharedPreferences);
         String init = sharedPreferences.getString(MainActivity.INITIAL_START, null);
         if (init == null || threePrefChanged) {
            initialStart();
            quizFragment.resetQuiz();
         } else {
            quizFragment.loadStateFromPreferences(PreferenceManager.getDefaultSharedPreferences(this));
            quizFragment.startQuiz();
         }
         quizFragment.setInitialLoadFalse();
         preferencesChanged = false;
         threePrefChanged = false;
      }
   }

   @Override
   protected void onPause() {
      super.onPause();

      updatePreferences();
   }

   // show menu if app is running on a phone or a portrait-oriented tablet
   @Override
   public boolean onCreateOptionsMenu(Menu menu) {
      // get the device's current orientation
      int orientation = getResources().getConfiguration().orientation;

      // display the app's menu only in portrait orientation
      if (orientation == Configuration.ORIENTATION_PORTRAIT) {
         // inflate the menu
         getMenuInflater().inflate(R.menu.menu_main, menu);
         return true;
      }
      else
         return false;
   }

   // displays the SettingsActivity when running on a phone
   @Override
   public boolean onOptionsItemSelected(MenuItem item) {
      Intent preferencesIntent = new Intent(this, SettingsActivity.class);
      startActivity(preferencesIntent);
      return super.onOptionsItemSelected(item);
   }

   // listener for changes to the app's SharedPreferences
   private OnSharedPreferenceChangeListener preferencesChangeListener =
      new OnSharedPreferenceChangeListener() {
         // called when the user changes the app's preferences
         @Override
         public void onSharedPreferenceChanged(
            SharedPreferences sharedPreferences, String key) {
            preferencesChanged = true; // user changed app setting

            if (key.equals(CHOICES)) { // # of choices to display changed
               threePrefChanged = true;
               int a = quizFragment.updateGuessRows(sharedPreferences);
               if (a == 1) {
                  Toast.makeText(MainActivity.this,
                          R.string.default_guesses_message,
                          Toast.LENGTH_SHORT).show();
               }

               Toast.makeText(MainActivity.this,
                       R.string.restarting_quiz,
                       Toast.LENGTH_SHORT).show();

               quizFragment.resetQuiz();
            }
            else if (key.equals(GUESSES)){
               threePrefChanged = true;
               int a = quizFragment.updateGuessAmount(sharedPreferences);
               if (a == 1) {
                  Toast.makeText(MainActivity.this,
                          R.string.default_guesses_message,
                          Toast.LENGTH_SHORT).show();
               }

               Toast.makeText(MainActivity.this,
                       R.string.restarting_quiz,
                       Toast.LENGTH_SHORT).show();

               quizFragment.resetQuiz();
            }
            else if (key.equals(REGIONS)) { // regions to include changed
               threePrefChanged = true;
               Set<String> regions =
                  sharedPreferences.getStringSet(REGIONS, null);

               if (regions != null && regions.size() > 0) {
                  quizFragment.updateRegions(sharedPreferences);
                  quizFragment.resetQuiz();
                  Toast.makeText(MainActivity.this,
                          R.string.restarting_quiz,
                          Toast.LENGTH_SHORT).show();
               }
               else {
                  // must select one region--set North America as default
                  SharedPreferences.Editor editor =
                     sharedPreferences.edit();
                  regions.add(getString(R.string.default_region));
                  editor.putStringSet(REGIONS, regions);
                  editor.apply();

                  Toast.makeText(MainActivity.this,
                     R.string.default_region_message,
                     Toast.LENGTH_SHORT).show();

                  Toast.makeText(MainActivity.this,
                          R.string.restarting_quiz,
                          Toast.LENGTH_SHORT).show();
               }
            }

         }
      };

   private void updatePreferences() {
      quizFragment.updateQuestionNum(PreferenceManager.getDefaultSharedPreferences(this));
      quizFragment.updateQuizList(PreferenceManager.getDefaultSharedPreferences(this));
      quizFragment.updateCorrectGuesses(PreferenceManager.getDefaultSharedPreferences(this));
      quizFragment.updateGuessesLeft(PreferenceManager.getDefaultSharedPreferences(this));
      quizFragment.updateTotalGuesses(PreferenceManager.getDefaultSharedPreferences(this));
      quizFragment.updateFileNameList(PreferenceManager.getDefaultSharedPreferences(this));
      quizFragment.updateCorrectAnswer(PreferenceManager.getDefaultSharedPreferences(this));
      quizFragment.updateButtonOptionsList(PreferenceManager.getDefaultSharedPreferences(this));
   }


   public void initialStart() {
      SharedPreferences sharedPreferences = PreferenceManager.getDefaultSharedPreferences(this);
      SharedPreferences.Editor editor =
              sharedPreferences.edit();
      String started = "started";
      editor.putString(MainActivity.INITIAL_START, started);
      editor.apply();
   }

   public void displayAddInfoFrag(String addInfoStr, int qNum) {
      // Create fragment and give it an argument specifying the article it should show
      AdditionalInfoFragment newFragment = new AdditionalInfoFragment();
      Bundle args = new Bundle();
      args.putString(AdditionalInfoFragment.ADD_INFO_REGION, addInfoStr.substring(0, addInfoStr.indexOf('-')));
      args.putString(AdditionalInfoFragment.ADD_INFO_FLAG, addInfoStr.substring(addInfoStr.indexOf('-') + 1));
      args.putInt(AdditionalInfoFragment.ADD_INFO_QNUM, qNum);
      newFragment.setArguments(args);

      FragmentTransaction transaction = getSupportFragmentManager().beginTransaction();

      transaction.add(R.id.quizFragment, newFragment);
      transaction.addToBackStack(null);

   // Commit the transaction
      transaction.commit();
   }
}


/*************************************************************************
 * (C) Copyright 1992-2016 by Deitel & Associates, Inc. and               *
 * Pearson Education, Inc. All Rights Reserved.                           *
 *                                                                        *
 * DISCLAIMER: The authors and publisher of this book have used their     *
 * best efforts in preparing the book. These efforts include the          *
 * development, research, and testing of the theories and programs        *
 * to determine their effectiveness. The authors and publisher make       *
 * no warranty of any kind, expressed or implied, with regard to these    *
 * programs or to the documentation contained in these books. The authors *
 * and publisher shall not be liable in any event for incidental or       *
 * consequential damages in connection with, or arising out of, the       *
 * furnishing, performance, or use of these programs.                     *
 *************************************************************************/
