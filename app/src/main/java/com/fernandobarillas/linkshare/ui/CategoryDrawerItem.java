package com.fernandobarillas.linkshare.ui;

import android.support.annotation.StringRes;

import com.mikepenz.materialdrawer.model.SecondaryDrawerItem;

/**
 * A DrawerItem for displaying {@link com.fernandobarillas.linkshare.models.Link} categories
 */

public class CategoryDrawerItem extends SecondaryDrawerItem {

    public CategoryDrawerItem(String category) {
        withName(category);
    }

    public CategoryDrawerItem(@StringRes int categoryStringRes) {
        withName(categoryStringRes);
    }

    @Override
    public String toString() {
        String name = "";
        if (getName() != null) name = getName().toString();
        return "CategoryDrawerItem{" + "name='" + name + '\'' + '}';
    }
}
