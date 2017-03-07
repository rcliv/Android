// MainActivityFragment.java
// Contains the Flag Quiz logic
package com.deitel.flagquiz;

import java.io.IOException;
import java.io.InputStream;
import java.security.SecureRandom;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.app.Activity;
import android.app.AlertDialog;
import android.app.Dialog;
import android.content.DialogInterface;
import android.content.SharedPreferences;
import android.content.res.AssetManager;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.os.Handler;
import android.preference.PreferenceManager;
import android.support.v4.app.DialogFragment;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentTransaction;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewAnimationUtils;
import android.view.ViewGroup;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

public class MainActivityFragment extends Fragment {
   // String used when logging error messages
   private static final String TAG = "FlagQuiz Activity";

   private static final int FLAGS_IN_QUIZ = 10;

   private List<String> fileNameList; // flag file names
   private List<String> quizCountriesList; // countries in current quiz
   private Set<String> regionsSet; // world regions in current quiz
   private String correctAnswer; // correct country for the current flag
   private int totalGuesses; // number of guesses made
   private int correctGuesses; // number of correct guesses
   private int guessRows; // number of rows displaying guess Buttons
   private SecureRandom random; // used to randomize the quiz
   private Handler handler; // used to delay loading next flag
   private Animation shakeAnimation; // animation for incorrect guess
   private int guessAmount; // amount of guesses allowed per flag
   private int guessesLeft; // amount of guesses left in this turn
   private int questionNum;
   private boolean initialLoad;
   private List<String> buttonOptionsList;

   private LinearLayout quizLinearLayout; // layout that contains the quiz
   private TextView questionNumberTextView; // shows current question #
   private ImageView flagImageView; // displays a flag
   private LinearLayout[] guessLinearLayouts; // rows of answer Buttons
   private TextView answerTextView; // displays correct answer

   DynamicFrag mCallback;

   public interface DynamicFrag {
      void displayAddInfoFrag(String addInfoStr, int qNum);
   }

   // configures the MainActivityFragment when its View is created
   @Override
   public View onCreateView(LayoutInflater inflater, ViewGroup container,
      Bundle savedInstanceState) {
      super.onCreateView(inflater, container, savedInstanceState);
      View view =
         inflater.inflate(R.layout.fragment_main, container, false);

      fileNameList = new ArrayList<>();
      quizCountriesList = new ArrayList<>();
      random = new SecureRandom();
      handler = new Handler();
      buttonOptionsList = new ArrayList<>();

      // load the shake animation that's used for incorrect answers
      shakeAnimation = AnimationUtils.loadAnimation(getActivity(),
         R.anim.incorrect_shake);
      shakeAnimation.setRepeatCount(3); // animation repeats 3 times

      // get references to GUI components
      quizLinearLayout =
         (LinearLayout) view.findViewById(R.id.quizLinearLayout);
      questionNumberTextView =
         (TextView) view.findViewById(R.id.questionNumberTextView);
      flagImageView = (ImageView) view.findViewById(R.id.flagImageView);
      guessLinearLayouts = new LinearLayout[4];
      guessLinearLayouts[0] =
         (LinearLayout) view.findViewById(R.id.row1LinearLayout);
      guessLinearLayouts[1] =
         (LinearLayout) view.findViewById(R.id.row2LinearLayout);
      guessLinearLayouts[2] =
         (LinearLayout) view.findViewById(R.id.row3LinearLayout);
      guessLinearLayouts[3] =
         (LinearLayout) view.findViewById(R.id.row4LinearLayout);
      answerTextView = (TextView) view.findViewById(R.id.answerTextView);

      // configure listeners for the guess Buttons
      for (LinearLayout row : guessLinearLayouts) {
         for (int column = 0; column < row.getChildCount(); column++) {
            Button button = (Button) row.getChildAt(column);
            button.setOnClickListener(guessButtonListener);
         }
      }

      // set questionNumberTextView's text
      questionNumberTextView.setText(
         getString(R.string.question, 1, FLAGS_IN_QUIZ));
      return view; // return the fragment's view for display
   }

   @Override
   public void onAttach(Activity activity)
   {
      super.onAttach(activity);

      try
      {
         mCallback = (DynamicFrag) activity;
      }
      catch (ClassCastException e)
      {
         throw new ClassCastException(activity.toString()
                 + " must implement OnHeadlineSelectedListener");
      }
   }

   // update guessRows based on value in SharedPreferences
   public int updateGuessRows(SharedPreferences sharedPreferences) {
      // get the number of guess buttons that should be displayed
      String choices =
         sharedPreferences.getString(MainActivity.CHOICES, null);
      int choiceAmount = Integer.parseInt(choices);
      guessRows = choiceAmount / 2;

      if (choiceAmount < guessAmount) {
         SharedPreferences.Editor editor =
                 sharedPreferences.edit();
         guessAmount = choiceAmount;
         String guesses = String.valueOf(guessAmount);
         editor.putString(MainActivity.GUESSES, guesses);
         editor.apply();
         return 1;
      }

      // hide all quess button LinearLayouts
      for (LinearLayout layout : guessLinearLayouts)
         layout.setVisibility(View.GONE);

      // display appropriate guess button LinearLayouts
      for (int row = 0; row < guessRows; row++)
         guessLinearLayouts[row].setVisibility(View.VISIBLE);

      return 0;
   }

   // update world regions for quiz based on values in SharedPreferences
   public void updateRegions(SharedPreferences sharedPreferences) {
      regionsSet =
         sharedPreferences.getStringSet(MainActivity.REGIONS, null);
   }

   public int updateGuessAmount(SharedPreferences sharedPreferences) {
      String guesses =
              sharedPreferences.getString(MainActivity.GUESSES, null);
      guessAmount = Integer.parseInt(guesses);

      String choices = sharedPreferences.getString(MainActivity.CHOICES, null);
      int choiceAmount = Integer.parseInt(choices);

      if (choiceAmount < guessAmount) {
         SharedPreferences.Editor editor =
                 sharedPreferences.edit();
         guessAmount = choiceAmount;
         guesses = String.valueOf(guessAmount);
         editor.putString(MainActivity.GUESSES, guesses);
         editor.apply();
         return 1;
      }
      return 0;
   }

   public void updateQuestionNum(SharedPreferences sharedPreferences) {
      SharedPreferences.Editor editor =
              sharedPreferences.edit();
      String qNum = String.valueOf(questionNum);
      editor.putString(MainActivity.QUESTION_NUM, qNum);
      editor.apply();
   }

   public void updateQuizList(SharedPreferences sharedPreferences) {
      SharedPreferences.Editor editor =
              sharedPreferences.edit();
      Set<String> a = new HashSet<>(quizCountriesList);
      editor.putStringSet(MainActivity.QUIZ_COUNTRIES, a);
      editor.apply();
   }

   public void updateCorrectGuesses(SharedPreferences sharedPreferences) {
      SharedPreferences.Editor editor = sharedPreferences.edit();
      String a = String.valueOf(correctGuesses);
      editor.putString(MainActivity.CORRECT_GUESSES, a);
      editor.apply();
   }

   public void updateTotalGuesses(SharedPreferences sharedPreferences) {
      SharedPreferences.Editor editor = sharedPreferences.edit();
      String a = String.valueOf(totalGuesses);
      editor.putString(MainActivity.TOTAL_GUESSES, a);
      editor.apply();
   }

   public void updateGuessesLeft(SharedPreferences sharedPreferences) {
      SharedPreferences.Editor editor = sharedPreferences.edit();
      String a = String.valueOf(guessesLeft);
      editor.putString(MainActivity.GUESSES_LEFT, a);
      editor.apply();
   }

   public void updateFileNameList(SharedPreferences sharedPreferences) {
      SharedPreferences.Editor editor = sharedPreferences.edit();
      Set<String> a = new HashSet<>(fileNameList);
      editor.putStringSet(MainActivity.FILE_NAME_LIST, a);
      editor.apply();
   }

   public void updateCorrectAnswer(SharedPreferences sharedPreferences) {
      SharedPreferences.Editor editor = sharedPreferences.edit();
      String a = correctAnswer;
      editor.putString(MainActivity.CORRECT_ANSWER, a);
      editor.apply();
   }

   public void updateButtonOptionsList(SharedPreferences sharedPreferences) {
      SharedPreferences.Editor editor = sharedPreferences.edit();
      Set<String> a = new HashSet<>(buttonOptionsList);
      editor.putStringSet(MainActivity.BUTTON_OPTIONS_LIST, a);
      editor.apply();
   }

   public void loadStateFromPreferences(SharedPreferences sharedPreferences) {
      String qNum = sharedPreferences.getString(MainActivity.QUESTION_NUM, null);
      if (!qNum.equals(null)) {
         questionNum = Integer.parseInt(qNum);
      }
      List<String> qCountries = new ArrayList<>(sharedPreferences.getStringSet(MainActivity.QUIZ_COUNTRIES, null));
      if (qCountries != null) {
         quizCountriesList = qCountries;
      }
      List<String> fNameList = new ArrayList<>(sharedPreferences.getStringSet(MainActivity.FILE_NAME_LIST, null));
      if (fNameList != null) {
         fileNameList = fNameList;
      }
      String cGuesses = sharedPreferences.getString(MainActivity.CORRECT_GUESSES, null);
      if (!cGuesses.equals(null)) {
         correctGuesses = Integer.parseInt(cGuesses);
      }
      String tGuesses = sharedPreferences.getString(MainActivity.TOTAL_GUESSES, null);
      if (!tGuesses.equals(null)) {
         totalGuesses = Integer.parseInt(tGuesses);
      }
      String gLeft = sharedPreferences.getString(MainActivity.GUESSES_LEFT, null);
      if (!gLeft.equals(null)) {
         guessesLeft = Integer.parseInt(gLeft);
      }
      String cAnswer = sharedPreferences.getString(MainActivity.CORRECT_ANSWER, null);
      if (!cAnswer.equals(null)) {
         correctAnswer = cAnswer;
      }
      List<String> bOptionsList = new ArrayList<>(sharedPreferences.getStringSet(MainActivity.BUTTON_OPTIONS_LIST, null));
      if (bOptionsList != null) {
         buttonOptionsList = bOptionsList;
      }
   }

   public void setInitialLoadTrue() {
      initialLoad = true;
   }

   public void setInitialLoadFalse() {
      initialLoad = false;
   }

   public void startQuiz() {
      loadNextFlag();
   }

   // set up and start the next quiz
   public void resetQuiz() {
      // use AssetManager to get image file names for enabled regions
      AssetManager assets = getActivity().getAssets();
      fileNameList.clear(); // empty list of image file names

      try {
         // loop through each region
         for (String region : regionsSet) {
            // get a list of all flag image files in this region
            String[] paths = assets.list(region);

            for (String path : paths)
               fileNameList.add(path.replace(".png", ""));
         }
      }
      catch (IOException exception) {
         Log.e(TAG, "Error loading image file names", exception);
      }

      correctAnswer = "";
      correctGuesses = 0; // reset the number of correct answers made
      questionNum = 1; // reset the question number
      totalGuesses = 0; // reset the total number of guesses the user made
      quizCountriesList.clear(); // clear prior list of quiz countries
      buttonOptionsList.clear();

      int flagCounter = 1;
      int numberOfFlags = fileNameList.size();

      // add FLAGS_IN_QUIZ random file names to the quizCountriesList
      while (flagCounter <= FLAGS_IN_QUIZ) {
         int randomIndex = random.nextInt(numberOfFlags);

         // get the random file name
         String filename = fileNameList.get(randomIndex);

         // if the region is enabled and it hasn't already been chosen
         if (!quizCountriesList.contains(filename)) {
            quizCountriesList.add(filename); // add the file to the list
            ++flagCounter;
         }
      }

      loadNextFlag(); // start the quiz by loading the first flag
   }

   // after the user guesses a correct flag, load the next flag
   private void loadNextFlag() {
      guessesLeft = guessAmount;

      String nextImage = "";

      if (correctAnswer.equals("")) {
         // get file name of the next flag and remove it from the list
         nextImage = quizCountriesList.get(0);
         correctAnswer = nextImage; // update the correct answer
         answerTextView.setText(""); // clear answerTextView
      } else {
         String a = quizCountriesList.get(0);
         int b = quizCountriesList.indexOf(correctAnswer);
         quizCountriesList.set(0, correctAnswer);
         quizCountriesList.set(b, a);
         nextImage = quizCountriesList.get(0);
         answerTextView.setText(""); // clear answerTextView
      }

      // display current question number
      questionNumberTextView.setText(getString(
         R.string.question, (questionNum), FLAGS_IN_QUIZ));

      // extract the region from the next image's name
      String region = nextImage.substring(0, nextImage.indexOf('-'));

      // use AssetManager to load next image from assets folder
      AssetManager assets = getActivity().getAssets();

      // get an InputStream to the asset representing the next flag
      // and try to use the InputStream
      try (InputStream stream =
              assets.open(region + "/" + nextImage + ".png")) {
         // load the asset as a Drawable and display on the flagImageView
         Drawable flag = Drawable.createFromStream(stream, nextImage);
         flagImageView.setImageDrawable(flag);

         animate(false); // animate the flag onto the screen
      }
      catch (IOException exception) {
         Log.e(TAG, "Error loading " + nextImage, exception);
      }

      Collections.shuffle(fileNameList); // shuffle file names

      // put the correct answer at the end of fileNameList
      int correct = fileNameList.indexOf(correctAnswer);
      fileNameList.add(fileNameList.remove(correct));

      List<String> buttonLabelList = new ArrayList<>();

      if (buttonOptionsList == null || buttonOptionsList.size() == 0) {
         buttonOptionsList = getButtonLabels(region);
         for (String i : buttonOptionsList) {
            buttonLabelList.add(i);
         }
      } else {
         for (String i : buttonOptionsList) {
            buttonLabelList.add(i);
         }
      }
      // add 2, 4, 6 or 8 guess Buttons based on the value of guessRows
      for (int row = 0; row < guessRows; row++) {
         // place Buttons in currentTableRow
         for (int column = 0;
              column < guessLinearLayouts[row].getChildCount();
              column++) {
            // get reference to Button to configure
            Button newGuessButton =
               (Button) guessLinearLayouts[row].getChildAt(column);
            newGuessButton.setEnabled(true);

            // get country name and set it as newGuessButton's text
            String filename = buttonLabelList.get(buttonLabelList.size() - 1);
            buttonLabelList.remove(filename);
            newGuessButton.setText(getCountryName(filename));
            newGuessButton.setCompoundDrawablesRelativeWithIntrinsicBounds(null, null, null, null);
         }
      }

      for (String i : buttonOptionsList) {
         if (i.equals(correctAnswer)) {
            return;
         }
      }

      // randomly replace one Button with the correct answer
      int row = random.nextInt(guessRows); // pick random row
      int column = random.nextInt(2); // pick random column
      //LinearLayout randomRow = guessLinearLayouts[row]; // get the row
      String countryName = getCountryName(correctAnswer);
      Button correctGuessButton = (Button) guessLinearLayouts[row].getChildAt(column);
      String cbText = String.valueOf(correctGuessButton.getText());
      String replacedAns = "";

      // find the region of the guessed country
      for (String i : fileNameList) {
         if (getCountryName(i).equals(cbText)) {
            replacedAns = i;
         }
      }
      if (buttonOptionsList != null || buttonOptionsList.size() != 0) {
         int a = buttonOptionsList.indexOf(replacedAns);
         buttonOptionsList.set(a, correctAnswer);
      }
      correctGuessButton.setText(countryName);
   }

   private List<String> getButtonLabels(String region) {
      List<String> copiedFileNameList = new ArrayList<>();
      for (String i : fileNameList) {
         copiedFileNameList.add(i);
      }
      copiedFileNameList.remove(correctAnswer);
      List<String> buttonLabels = new ArrayList<>();
      int sameRegionCount = guessRows + 1;
      String filename;
      for (int a = 0; a < guessRows*2; a++) {
         int i = random.nextInt(copiedFileNameList.size());
         filename = copiedFileNameList.get(i);
         copiedFileNameList.remove(filename);
         if (sameRegionCount != 0) {
            while (!filename.substring(0, filename.indexOf('-')).equals(region)) {
               i = random.nextInt(copiedFileNameList.size());
               filename = copiedFileNameList.get(i);
               copiedFileNameList.remove(filename);
            }
            sameRegionCount--;
            buttonLabels.add(filename);
         } else {
            buttonLabels.add(filename);
         }
      }
      return buttonLabels;
   }

   // parses the country flag file name and returns the country name
   private String getCountryName(String name) {
      return name.substring(name.indexOf('-') + 1).replace('_', ' ');
   }

   // animates the entire quizLinearLayout on or off screen
   private void animate(boolean animateOut) {
      // prevent animation into the the UI for the first flag
      if (initialLoad == true)
         return;

      // calculate center x and center y
      int centerX = (quizLinearLayout.getLeft() +
         quizLinearLayout.getRight()) / 2; // calculate center x
      int centerY = (quizLinearLayout.getTop() +
         quizLinearLayout.getBottom()) / 2; // calculate center y

      // calculate animation radius
      int radius = Math.max(quizLinearLayout.getWidth(),
         quizLinearLayout.getHeight());

      Animator animator;

      // if the quizLinearLayout should animate out rather than in
      if (animateOut) {
         // create circular reveal animation
         animator = ViewAnimationUtils.createCircularReveal(
            quizLinearLayout, centerX, centerY, radius, 0);
         animator.addListener(
            new AnimatorListenerAdapter() {
               // called when the animation finishes
               @Override
               public void onAnimationEnd(Animator animation) {
                  loadNextFlag();
               }
            }
         );
      }
      else { // if the quizLinearLayout should animate in
         animator = ViewAnimationUtils.createCircularReveal(
            quizLinearLayout, centerX, centerY, 0, radius);
      }

      animator.setDuration(500); // set animation duration to 500 ms
      animator.start(); // start the animation
   }

   // called when a guess Button is touched
   private OnClickListener guessButtonListener = new OnClickListener() {
      @Override
      public void onClick(View v) {
         Button guessButton = ((Button) v);
         String guess = guessButton.getText().toString();
         String answer = getCountryName(correctAnswer);
         ++totalGuesses; // increment number of guesses the user has made

         setButtonFlagImage(guessButton, guess);

         if (guess.equals(answer)) { // if the guess is correct
            ++correctGuesses; // increment the number of correct answers
            final String addInfoStr = quizCountriesList.remove(0);
            correctAnswer = "";
            buttonOptionsList.clear();

            // display correct answer in green text
            answerTextView.setText(answer + "!");
            answerTextView.setTextColor(
               getResources().getColor(R.color.correct_answer,
                  getContext().getTheme()));

            disableButtons(); // disable all guess Buttons
            // if the user has correctly identified FLAGS_IN_QUIZ flags
            if (questionNum == FLAGS_IN_QUIZ) {
               endGame();
            }
            else { // answer is correct but quiz is not over

               //mCallback.displayAddInfoFrag(addInfoStr);

               // load the next flag after a 2-second delay
               handler.postDelayed(
                  new Runnable() {
                     @Override
                     public void run() {
                        animate(true); // animate the flag off the screen
                        mCallback.displayAddInfoFrag(addInfoStr, questionNum);
                        questionNum++;
                     }
                  }, 2000); // 2000 milliseconds for 2-second delay
            }
         }
         else { // answer was incorrect
            flagImageView.startAnimation(shakeAnimation); // play shake

            guessesLeft--;
            if (guessesLeft == 0) {
               final String addInfoStr = quizCountriesList.remove(0);
               correctAnswer = "";
               buttonOptionsList.clear();

               answerTextView.setText("The correct answer is " + answer + "!");
               answerTextView.setTextColor(getResources().getColor(
                       R.color.incorrect_answer, getContext().getTheme()));
               guessButton.setEnabled(false); // disable incorrect answer
               disableButtons();
               if (questionNum == FLAGS_IN_QUIZ) {
                  endGame();
               } else {

                  //mCallback.displayAddInfoFrag(addInfoStr);

                  handler.postDelayed(
                          new Runnable() {
                             @Override
                             public void run() {
                                animate(true); // animate the flag off the screen
                                mCallback.displayAddInfoFrag(addInfoStr, questionNum);
                                questionNum++;
                             }
                          }, 2000);
               }
            } else {
               // display "Incorrect!" in red
               answerTextView.setText(R.string.incorrect_answer);
               answerTextView.setTextColor(getResources().getColor(
                       R.color.incorrect_answer, getContext().getTheme()));
               guessButton.setEnabled(false); // disable incorrect answer
            }
         }
      }
   };

   private void endGame() {
      // DialogFragment to display quiz stats and start new quiz
      DialogFragment quizResults =

              new DialogFragment() {
                 // create an AlertDialog and return it
                 @Override
                 public Dialog onCreateDialog(Bundle bundle) {
                    AlertDialog.Builder builder =
                            new AlertDialog.Builder(getActivity());
                    builder.setMessage(
                            getString(R.string.results,
                                    correctGuesses, totalGuesses,
                                    (100 * correctGuesses / (double) totalGuesses)));

                    // "Reset Quiz" Button
                    builder.setPositiveButton(R.string.reset_quiz,
                            new DialogInterface.OnClickListener() {
                               public void onClick(DialogInterface dialog,
                                                   int id) {
                                  resetQuiz();
                               }
                            }
                    );

                    return builder.create(); // return the AlertDialog
                 }
              };

      // use FragmentManager to display the DialogFragment
      quizResults.setCancelable(false);
      quizResults.show(getFragmentManager(), "quiz results");
   }

   private void setButtonFlagImage(Button guessButton, String guess) {
      // use AssetManager to load flag image from assets folder
      AssetManager assets = getActivity().getAssets();
      String guessRegion = "";

      // find the region of the guessed country
      for (String i : fileNameList) {
         if (getCountryName(i).equals(guess)) {
            guessRegion = i.substring(0, i.indexOf("-"));
         }
      }

      // format the flag path
      String guessFlag = (guessRegion + "/" + guessRegion + "-" + guess.replace(" ", "_") + ".png");

      //display chosen answer flag
      try (InputStream stream =
                   assets.open(guessFlag)) {
         // load the asset as a Drawable and display on the flagImageView
         Drawable flag = Drawable.createFromStream(stream, guess);
         guessButton.setCompoundDrawablesWithIntrinsicBounds(null, null, flag, null);

      }
      catch (IOException exception) {
         Log.e(TAG, "Error loading " + guess, exception);
      }
   }

   // utility method that disables all answer Buttons
   private void disableButtons() {
      for (int row = 0; row < guessRows; row++) {
         LinearLayout guessRow = guessLinearLayouts[row];
         for (int i = 0; i < guessRow.getChildCount(); i++)
            guessRow.getChildAt(i).setEnabled(false);
      }
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
