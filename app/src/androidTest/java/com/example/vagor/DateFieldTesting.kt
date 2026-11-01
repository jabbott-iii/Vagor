package com.example.vagor

import android.view.View
import androidx.test.espresso.UiController
import androidx.test.espresso.ViewAction
import androidx.test.espresso.matcher.BoundedMatcher
import androidx.test.espresso.matcher.ViewMatchers.isAssignableFrom
import org.hamcrest.Description
import org.hamcrest.Matcher

// Puts a stored MM/dd/yyyy date (or an older loose one) in a date field, as picking it from the
// calendar would. The field shows it in the phone's format.
fun setDate(stored: String): ViewAction = object : ViewAction {
    override fun getConstraints(): Matcher<View> = isAssignableFrom(DateField::class.java)

    override fun getDescription() = "set the date to $stored"

    override fun perform(uiController: UiController, view: View) {
        (view as DateField).date = stored
    }
}

// A date field holding [stored], whatever format it's shown in.
fun withDate(stored: String): Matcher<View> = object : BoundedMatcher<View, DateField>(DateField::class.java) {
    override fun describeTo(description: Description) {
        description.appendText("with the date $stored")
    }

    override fun matchesSafely(item: DateField) = item.date == stored
}
