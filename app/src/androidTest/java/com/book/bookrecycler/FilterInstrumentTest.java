package com.book.bookrecycler;


import androidx.test.ext.junit.rules.ActivityScenarioRule;

import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;

import static androidx.test.espresso.Espresso.onData;
import static androidx.test.espresso.Espresso.onView;
import static androidx.test.espresso.action.ViewActions.click;
import static androidx.test.espresso.matcher.RootMatchers.isPlatformPopup;
import static androidx.test.espresso.matcher.ViewMatchers.withId;
import static org.hamcrest.Matchers.allOf;
import static org.hamcrest.Matchers.instanceOf;
import static org.hamcrest.Matchers.is;

@RunWith(androidx.test.ext.junit.runners.AndroidJUnit4.class)
public class FilterInstrumentTest {

    //Condition and category to test
    public String condition = "New";
    public String category = "Computer";

    @Rule
    public ActivityScenarioRule<MainActivity> mMainActivity =
            new ActivityScenarioRule <>(MainActivity.class);

    @Test
    public void testFiltering(){

        onView(withId(R.id.filterBtn)).perform(click());

        onView(withId(R.id.bottom_sheet_condition_spinner)).perform(click());
        onData(allOf(is(instanceOf(String.class)),
                is(condition)))
                .inRoot(isPlatformPopup())
                .perform(click());


        onView(withId(R.id.bottom_sheet_category_spinner)).perform(click());
        onData(allOf(is(instanceOf(String.class)),
                is(category)))
                .inRoot(isPlatformPopup())
                .perform(click());

        onView(withId(R.id.bottom_sheet_apply_btn)).perform(click());

        //sleep to see the results in RV
        try {
            Thread.sleep(3000);
        } catch (InterruptedException e) {
           e.printStackTrace();
        }
    }

}
