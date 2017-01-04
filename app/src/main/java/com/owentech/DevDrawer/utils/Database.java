package com.owentech.DevDrawer.utils;

import android.appwidget.AppWidgetManager;
import android.content.ComponentName;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.util.SparseArray;

import com.owentech.DevDrawer.appwidget.DDWidgetProvider;
import com.owentech.DevDrawer.data.OpenHelper;
import com.owentech.DevDrawer.data.model.App;
import com.owentech.DevDrawer.data.model.AppModel;
import com.owentech.DevDrawer.data.model.Filter;
import com.owentech.DevDrawer.data.model.FilterModel;
import com.owentech.DevDrawer.data.model.Widget;
import com.owentech.DevDrawer.data.model.WidgetModel;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Callable;

import javax.inject.Inject;

public class Database {

    Context context;
    OpenHelper openHelper;

    @Inject
    public Database(Context context){
        this.context = context;
        openHelper = new OpenHelper(this.context);
    }

    public static int NOT_FOUND = 1000000;

    public Callable<Boolean> addWidgetToDatabase(final long widgetId, final String name) {
        return new Callable<Boolean>() {
            @Override
            public Boolean call() throws Exception {
                Widget.AddWiget addWiget = new WidgetModel.AddWiget(openHelper.getWritableDatabase());
                addWiget.bind(widgetId, name);
                addWiget.program.execute();
                return true;
            }
        };
    }

    public Callable<Boolean> renameWidget(final long widgetId, final String name) {
        return new Callable<Boolean>() {
            @Override
            public Boolean call() throws Exception {
                Widget.RenameWidget renameWidget = new WidgetModel.RenameWidget(openHelper.getWritableDatabase());
                renameWidget.bind(name, widgetId);
                renameWidget.program.execute();
                return true;
            }
        };
    }

    public Callable<Boolean> removeWidgetFromDatabase(final long widgetId) {
        return new Callable<Boolean>() {
            @Override
            public Boolean call() throws Exception {
                SQLiteDatabase db = openHelper.getWritableDatabase();

                WidgetModel.RemoveWidget removeWidget = new WidgetModel.RemoveWidget(db);
                removeWidget.bind(widgetId);
                removeWidget.program.execute();

                FilterModel.DeleteFiltersForWidgetId deleteFiltersForWidgetId = new FilterModel.DeleteFiltersForWidgetId(db);
                deleteFiltersForWidgetId.bind(widgetId);
                deleteFiltersForWidgetId.program.execute();

                AppModel.DeleteAppsForWidgetId deleteAppsForWidgetId = new AppModel.DeleteAppsForWidgetId(db);
                deleteAppsForWidgetId.bind(widgetId);
                deleteAppsForWidgetId.program.execute();
                return true;
            }
        };
    }

    // TODO: 28/12/2016 re-write
    public Callable<SparseArray<String>> getWidgetNames(final Context context) {
        return new Callable<SparseArray<String>>() {
            @Override
            public SparseArray<String> call() throws Exception {
                SparseArray<String> result = new SparseArray<>();

                Cursor cursor = openHelper.getWritableDatabase().rawQuery(Widget.SELECTALLWIDGETS, new String[0]);
                cursor.moveToFirst();

                AppWidgetManager appWidgetManager = AppWidgetManager.getInstance(context);
                int[] ids = appWidgetManager.getAppWidgetIds(new ComponentName(context, DDWidgetProvider.class));

                while (!cursor.isAfterLast()) {

                    String name = cursor.getString(1);
                    if (name == null || name.length() == 0){
                        name = AppConstants.UNNAMED;
                    }

                    boolean exists = false;
                    for (int i : ids){
                        if (cursor.getInt(0) == i){
                            exists = true;
                        }
                    }

                    if (exists) {
                        result.put(cursor.getInt(0), name);
                    }
                    else{
                        RxUtils.fromCallable(removeWidgetFromDatabase(cursor.getInt(0)))
                                .subscribe();
                    }
                    cursor.moveToNext();
                }

                cursor.close();
                return result;
            }
        };
    }

    public Callable<Boolean> addFilterToDatabase(final String packageFilter, final long widgetId) {
        return new Callable<Boolean>() {
            @Override
            public Boolean call() throws Exception {
                Filter.InsertFilter insertFilter = new FilterModel.InsertFilter(openHelper.getWritableDatabase());
                insertFilter.bind(packageFilter, widgetId);
                insertFilter.program.execute();
                return true;
            }
        };
    }

    public Callable<Boolean> addAppToDatabase(final String packageFilter, final long filterId, final long widgetId) {
        return new Callable<Boolean>() {
            @Override
            public Boolean call() throws Exception {
                App.InsertApp insertApp = new AppModel.InsertApp(openHelper.getWritableDatabase());
                insertApp.bind(packageFilter, filterId, widgetId);
                insertApp.program.execute();
                return true;
            }
        };
    }

    public Callable<Boolean> removeFilterFromDatabase(final long i) {
        return new Callable<Boolean>() {
            @Override
            public Boolean call() throws Exception {
                Filter.RemoveFilter removeFilter = new FilterModel.RemoveFilter(openHelper.getWritableDatabase());
                removeFilter.bind(i);
                removeFilter.program.execute();
                return true;
            }
        };
    }

    public Callable<Boolean> removeAppFromDatabase(final long filterId) {
        return new Callable<Boolean>() {
            @Override
            public Boolean call() throws Exception {
                App.RemoveApp removeApp = new AppModel.RemoveApp(openHelper.getWritableDatabase());
                removeApp.bind(filterId);
                removeApp.program.execute();
                return true;
            }
        };
    }

    public Callable<List<Filter>> getAllFiltersInDatabase() {

        return new Callable<List<Filter>>() {
            @Override
            public List<Filter> call() throws Exception {
                Cursor cursor = openHelper.getWritableDatabase().rawQuery(Filter.SELECTALLFILTERS, new String[0]);
                List<Filter> filters = new ArrayList<>();

                cursor.moveToFirst();

                while (!cursor.isAfterLast()) {
                    Filter filter = Filter.MAPPER.map(cursor);
                    filters.add(filter);
                    cursor.moveToNext();
                }

                cursor.close();
                return filters;
            }
        };
    }

    public Callable<List<Filter>> getAllFiltersInDatabase(final int widgetId) {

        return new Callable<List<Filter>>() {
            @Override
            public List<Filter> call() throws Exception {
                Cursor cursor = openHelper.getWritableDatabase().rawQuery(Filter.SELECTFILTERSFORWIDGETID, new String[]{String.valueOf(widgetId)});
                List<Filter> filters = new ArrayList<>();

                cursor.moveToFirst();

                while (!cursor.isAfterLast()) {
                    Filter filter = Filter.MAPPER.map(cursor);
                    filters.add(filter);
                    cursor.moveToNext();
                }

                cursor.close();
                return filters;
            }
        };
    }

    public String[] getAllAppsInDatabase(int widgetId) {
        String[] packages;

        Cursor cursor = openHelper.getWritableDatabase().rawQuery(App.SELECTALLAPPSFORWIDGETID, new String[]{String.valueOf(widgetId)});
        cursor.moveToFirst();
        packages = new String[cursor.getCount()];

        int i = 0;
        while (!cursor.isAfterLast()) {
            App app = App.MAPPER.map(cursor);
            packages[i] = app.package_();
            i++;
            cursor.moveToNext();
        }

        cursor.close();
        return packages;
    }

    public int getFiltersCount() {
        Cursor cursor = openHelper.getWritableDatabase().rawQuery(Filter.GETFILTERCOUNT, new String[0]);
        cursor.moveToFirst();
        int count = cursor.getInt(0);
        cursor.close();
        return count;
    }

    public boolean doesFilterExist(String s, int appWidgetId) {
        Cursor cursor = openHelper.getWritableDatabase().rawQuery(Filter.GETFILTERCOUNTFORPACKAGEANDWIDGETID, new String[]{s, String.valueOf(appWidgetId)});

        // get number of rows
        cursor.moveToFirst();
        int count = cursor.getInt(0);
        cursor.close();

        return count != 0;
    }

    public int getAppsCount() {
        Cursor cursor = openHelper.getWritableDatabase().rawQuery(App.GETAPPSCOUNT, new String[0]);
        cursor.moveToFirst();
        int count = cursor.getInt(0);
        cursor.close();
        return count;
    }

    public boolean doesAppExistInDb(String s) {
        Cursor cursor = openHelper.getWritableDatabase().rawQuery(App.GETAPPSCOUNTFORPACKAGE, new String[]{s});
        int count = cursor.getCount();
        cursor.close();
        return count != 0;
    }

    public void deleteAppFromDb(String packageName) {
        App.DeleteAppsForPackage deleteAppsForPackage = new AppModel.DeleteAppsForPackage(openHelper.getWritableDatabase());
        deleteAppsForPackage.bind(packageName);
        deleteAppsForPackage.program.execute();
    }

    // TODO: 28/12/2016 re-write, disgusting.
    public long parseAndMatch(String p, int widgetId) {

        long match = NOT_FOUND;

        Cursor cursor = openHelper.getWritableDatabase().rawQuery(Filter.SELECTFILTERSFORWIDGETID, new String[]{p});
        cursor.moveToFirst();

        while (!cursor.isAfterLast()) {
            Filter filter = Filter.MAPPER.map(cursor);
            String packageFilter = filter.package_();

            if (packageFilter.contains("*")) {
                if (p.toLowerCase().startsWith(packageFilter.toLowerCase().substring(0, packageFilter.indexOf("*")))) {
                    match = filter.id();
                }
            }
            else {
                if (p.toLowerCase().equals(packageFilter.toLowerCase())) {
                    match = filter.id();
                }
            }
            cursor.moveToNext();
        }

        cursor.close();
        return match;
    }

    public void amendFilterEntryTo(long id, String newString) {
        Filter.AmendFilter amendFilter = new FilterModel.AmendFilter(openHelper.getWritableDatabase());
        amendFilter.bind(newString, id);
        amendFilter.program.execute();
    }

    public void cleanWidgets(List<Integer> widgets){
        String widgetsArrayString = createInArray(widgets);
        openHelper.getWritableDatabase().rawQuery("DELETE FROM devdrawer_widgets WHERE id NOT IN " + widgetsArrayString, new String[0]);
        openHelper.getWritableDatabase().rawQuery("DELETE FROM devdrawer_filter WHERE widgetid NOT IN " + widgetsArrayString, new String[0]);
        openHelper.getWritableDatabase().rawQuery("DELETE FROM devdrawer_app WHERE widgetid NOT IN " + widgetsArrayString, new String[0]);
    }

    private String createInArray(List<Integer> widgets){
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append("(");
        for (Integer widget : widgets) {
            if (stringBuilder.length() != 1){
                stringBuilder.append(", ");
            }
            stringBuilder.append("\'");
            stringBuilder.append(Integer.toString(widget));
            stringBuilder.append("\'");
        }
        stringBuilder.append(")");
        return stringBuilder.toString();
    }
}
