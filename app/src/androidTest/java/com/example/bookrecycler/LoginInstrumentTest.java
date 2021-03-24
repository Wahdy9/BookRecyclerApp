package com.example.bookrecycler;

import android.util.Log;
import android.widget.EditText;

import androidx.test.rule.ActivityTestRule;
import androidx.test.runner.AndroidJUnit4;

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

@RunWith(AndroidJUnit4.class)
public class LoginInstrumentTest {

    //Email and password to test
    private String EMAIL = "test2@gmail.com";
    private String PASSWORD = "123456";

    @Before
    public void signOutFirst(){
        FirebaseAuth.getInstance().signOut();
    }

    @Rule
    public ActivityTestRule<LoginAndRegisterActivity> mLoginActivity =
            new ActivityTestRule<>(LoginAndRegisterActivity.class);

    @Test
    public void testLoginBtnClick() {
        if(Utils.isConnectedToInternet(mLoginActivity.getActivity())) {

            onView(allOf(
                    isDescendantOfA(withId(R.id.log_email_et)),
                    isAssignableFrom(EditText.class))).perform(typeText(EMAIL));
            onView(allOf(
                    isDescendantOfA(withId(R.id.log_pass_et)),
                    isAssignableFrom(EditText.class))).perform(typeText(PASSWORD), closeSoftKeyboard());
            onView(withId(R.id.log_btn)).perform(click());

        }else{
            Log.d("Test Login", "No Internet ");
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
