package com.book.bookrecycler;

import android.util.Log;
import android.widget.EditText;

import androidx.appcompat.app.AppCompatActivity;
import androidx.test.ext.junit.rules.ActivityScenarioRule;

import com.google.firebase.auth.FirebaseAuth;

import org.junit.After;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;

import static androidx.test.espresso.Espresso.onView;
import static androidx.test.espresso.action.ViewActions.click;
import static androidx.test.espresso.action.ViewActions.closeSoftKeyboard;
import static androidx.test.espresso.action.ViewActions.typeText;
import static androidx.test.espresso.matcher.ViewMatchers.isAssignableFrom;
import static androidx.test.espresso.matcher.ViewMatchers.isDescendantOfA;
import static androidx.test.espresso.matcher.ViewMatchers.withId;
import static org.hamcrest.Matchers.allOf;

@RunWith(androidx.test.ext.junit.runners.AndroidJUnit4.class)
public class LoginInstrumentTest extends AppCompatActivity {

    //Email and password to test
    private String EMAIL = "muhdkhamis1@gmail.com";
    private String PASSWORD = "123456";

    @Before
    public void signOutFirst(){
        FirebaseAuth.getInstance().signOut();
    }

    @Rule
    public ActivityScenarioRule<LoginAndRegisterActivity> mLoginActivity =
            new ActivityScenarioRule<>(LoginAndRegisterActivity.class);

    @Test
    public void testLoginBtnClick() {


            onView(allOf(
                    isDescendantOfA(withId(R.id.log_email_et)),
                    isAssignableFrom(EditText.class))).perform(typeText(EMAIL),closeSoftKeyboard());

            onView(allOf(
                    isDescendantOfA(withId(R.id.log_pass_et)),
                    isAssignableFrom(EditText.class))).perform(typeText(PASSWORD),closeSoftKeyboard());

                onView(withId(R.id.log_btn)).perform(click());
            //to wait for the completion of the test
            try {
                Thread.sleep(3000);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }


    }

    @After
    public void afterTesting(){
        if(FirebaseAuth.getInstance().getCurrentUser() !=null){
            Log.d("LoginTest", "Success");
        }else{
            Log.d("LoginTest", "Fail");
        }
    }
}
