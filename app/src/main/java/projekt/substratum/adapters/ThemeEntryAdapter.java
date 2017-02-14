package projekt.substratum.adapters;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.NotificationManager;
import android.app.ProgressDialog;
import android.content.ActivityNotFoundException;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Vibrator;
import android.preference.PreferenceManager;
import android.support.design.widget.Snackbar;
import android.support.v7.widget.CardView;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

import projekt.substratum.R;
import projekt.substratum.config.References;
import projekt.substratum.model.ThemeInfo;
import projekt.substratum.util.ReadOverlays;

public class ThemeEntryAdapter extends RecyclerView.Adapter<ThemeEntryAdapter.ViewHolder> {
    private ArrayList<ThemeInfo> information;
    private Context mContext;
    private ProgressDialog mProgressDialog;
    private ThemeInfo currentObject;
    private Activity mActivity;

    public ThemeEntryAdapter(ArrayList<ThemeInfo> information) {
        this.information = information;
    }

    @Override
    public ThemeEntryAdapter.ViewHolder onCreateViewHolder(ViewGroup viewGroup, int i) {
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(viewGroup
                .getContext());
        View view;
        if (prefs.getBoolean("nougat_style_cards", false)) {
            view = LayoutInflater.from(viewGroup.getContext()).inflate(R.layout.theme_entry_card_n,
                    viewGroup, false);
        } else {
            view = LayoutInflater.from(viewGroup.getContext()).inflate(R.layout.theme_entry_card,
                    viewGroup, false);
        }
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(ViewHolder viewHolder, int pos) {
        final int i = pos;
        mContext = information.get(i).getContext();
        mActivity = information.get(i).getActivity();
        viewHolder.theme_name.setText(information.get(i).getThemeName());
        viewHolder.theme_author.setText(information.get(i).getThemeAuthor());
        if (information.get(i).getPluginVersion() != null) {
            viewHolder.plugin_version.setText(information.get(i).getPluginVersion());
        } else {
            viewHolder.plugin_version.setVisibility(View.INVISIBLE);
        }
        if (information.get(i).getSDKLevels() != null) {
            viewHolder.theme_apis.setText(information.get(i).getSDKLevels());
        } else {
            viewHolder.theme_apis.setVisibility(View.INVISIBLE);
        }
        if (information.get(i).getThemeVersion() != null) {
            viewHolder.theme_version.setText(information.get(i).getThemeVersion());
        } else {
            viewHolder.theme_version.setVisibility(View.INVISIBLE);
        }
        if (information.get(i).getThemeReadyVariable() == null) {
            viewHolder.divider.setVisibility(View.GONE);
            viewHolder.tbo.setVisibility(View.GONE);
            viewHolder.two.setVisibility(View.GONE);
        } else if (information.get(i).getThemeReadyVariable().equals("all")) {
            viewHolder.divider.setVisibility(View.VISIBLE);
            viewHolder.tbo.setVisibility(View.VISIBLE);
            viewHolder.two.setVisibility(View.VISIBLE);
        } else if (information.get(i).getThemeReadyVariable().equals("ready")) {
            viewHolder.divider.setVisibility(View.VISIBLE);
            viewHolder.tbo.setVisibility(View.VISIBLE);
            viewHolder.two.setVisibility(View.GONE);
        } else if (information.get(i).getThemeReadyVariable().equals("stock")) {
            viewHolder.divider.setVisibility(View.VISIBLE);
            viewHolder.tbo.setVisibility(View.GONE);
            viewHolder.two.setVisibility(View.VISIBLE);
        } else {
            viewHolder.divider.setVisibility(View.GONE);
            viewHolder.tbo.setVisibility(View.GONE);
            viewHolder.two.setVisibility(View.GONE);
        }

        viewHolder.cardView.setOnClickListener(
                v -> {
                    SharedPreferences prefs1 = information.get(i).getContext()
                            .getSharedPreferences(
                                    "substratum_state", Context.MODE_PRIVATE);
                    if (!prefs1.contains("is_updating")) prefs1.edit()
                            .putBoolean("is_updating", false).apply();
                    if (!prefs1.getBoolean("is_updating", true)) {
                        // Process fail case if user uninstalls an app and goes back an activity
                        if (References.isPackageInstalled(information.get(i).getContext(),
                                information.get(i).getThemePackage())) {

                            File checkSubstratumVerity = new File(information.get(i)
                                    .getContext().getCacheDir()
                                    .getAbsoluteFile() + "/SubstratumBuilder/" +
                                    information.get(i).getThemePackage() + "/substratum.xml");
                            if (checkSubstratumVerity.exists()) {
                                References.launchTheme(information.get(i).getContext(),
                                        information.get(i)
                                                .getThemePackage(), information.get(i)
                                                .getThemeMode(), false);
                            } else {
                                new References.SubstratumThemeUpdate(
                                        information.get(i).getContext(),
                                        information.get(i).getThemePackage(),
                                        information.get(i).getThemeName(),
                                        information.get(i).getThemeMode())
                                        .execute();
                            }
                        } else {
                            Snackbar.make(v,
                                    information.get(pos).getContext()
                                            .getString(R.string.toast_uninstalled),
                                    Snackbar.LENGTH_LONG)
                                    .show();
                            information.get(i).getActivity().recreate();
                        }
                    } else {
                        if (References.isNotificationVisible(
                                mContext, References.notification_id_upgrade)) {
                            Snackbar.make(v,
                                    information.get(pos).getContext()
                                            .getString(R.string.background_updating_toast),
                                    Snackbar.LENGTH_LONG)
                                    .show();
                        } else {
                            Snackbar.make(v,
                                    information.get(pos).getContext()
                                            .getString(R.string.background_needs_invalidating),
                                    Snackbar.LENGTH_INDEFINITE)
                                    .setAction(mContext.getString(
                                            R.string.background_needs_invalidating_button),
                                            view -> new deleteCache().execute(""))
                                    .show();
                        }
                    }
                });

        viewHolder.cardView.setOnLongClickListener(view -> {
            // Vibrate the device alerting the user they are about to do something dangerous!
            Vibrator v = (Vibrator) information.get(i).getContext()
                    .getSystemService(Context.VIBRATOR_SERVICE);
            v.vibrate(30);

            android.support.v7.app.AlertDialog.Builder builder = new
                    android.support.v7.app.AlertDialog.Builder(information.get(i).getContext());
            builder.setTitle(information.get(i).getThemeName());
            builder.setIcon(References.grabAppIcon(information.get(i).getContext(),
                    information.get(i).getThemePackage()));
            builder.setMessage(R.string.uninstall_dialog_body)
                    .setPositiveButton(R.string.uninstall_dialog_okay, (dialog, id) -> {
                        mContext = information.get(i).getContext();
                        currentObject = information.get(i);
                        new uninstallTheme().execute();
                    })
                    .setNegativeButton(R.string.uninstall_dialog_cancel, (dialog, id) -> dialog
                            .cancel());
            // Create the AlertDialog object and return it
            builder.create();
            builder.show();
            return false;
        });

        viewHolder.tbo.setOnClickListener(
                v -> new AlertDialog.Builder(information.get(i)
                        .getContext())
                        .setMessage(R.string.tbo_description)
                        .setPositiveButton(R.string.tbo_dialog_proceed,
                                (dialog, which) -> {
                                    try {
                                        String playURL =
                                                information.get(i).getContext()
                                                        .getString(R.string
                                                                .tbo_theme_ready_url);
                                        Intent intent = new Intent(Intent.ACTION_VIEW);
                                        intent.setData(Uri.parse(playURL));
                                        information.get(i).getContext()
                                                .startActivity(intent);
                                    } catch (ActivityNotFoundException
                                            activityNotFoundException) {
                                        // Suppress warning
                                    }
                                })
                        .setNegativeButton(android.R.string.cancel,
                                (dialog, which) -> dialog.cancel())
                        .setCancelable(true)
                        .show()
        );

        viewHolder.two.setOnClickListener(
                v -> new AlertDialog.Builder(information.get(i)
                        .getContext())
                        .setMessage(R.string.two_description)
                        .setCancelable(true)
                        .show()
        );

        viewHolder.theme_author.setText(information.get(i).getThemeAuthor());
        viewHolder.imageView.setImageDrawable(information.get(i).getThemeDrawable());
    }

    @Override
    public int getItemCount() {
        return information.size();
    }

    class ViewHolder extends RecyclerView.ViewHolder {
        CardView cardView;
        TextView theme_name;
        TextView theme_author;
        TextView theme_apis;
        TextView theme_version;
        TextView plugin_version;
        ImageView imageView;
        View divider;
        ImageView tbo;
        ImageView two;

        ViewHolder(View view) {
            super(view);
            cardView = (CardView) view.findViewById(R.id.theme_card);
            theme_name = (TextView) view.findViewById(R.id.theme_name);
            theme_author = (TextView) view.findViewById(R.id.theme_author);
            theme_apis = (TextView) view.findViewById(R.id.api_levels);
            theme_version = (TextView) view.findViewById(R.id.theme_version);
            plugin_version = (TextView) view.findViewById(R.id.plugin_version);
            imageView = (ImageView) view.findViewById(R.id.theme_preview_image);
            divider = view.findViewById(R.id.theme_ready_divider);
            tbo = (ImageView) view.findViewById(R.id.theme_ready_indicator);
            two = (ImageView) view.findViewById(R.id.theme_unready_indicator);
        }
    }

    private class uninstallTheme extends AsyncTask<String, Integer, String> {
        @Override
        protected void onPreExecute() {
            String parseMe = String.format(mContext.getString(R.string.adapter_uninstalling),
                    currentObject.getThemeName());
            mProgressDialog = new ProgressDialog(mContext);
            mProgressDialog.setMessage(parseMe);
            mProgressDialog.setIndeterminate(true);
            mProgressDialog.setCancelable(false);
            mProgressDialog.show();
            // Clear the notification of building theme if shown
            NotificationManager manager = (NotificationManager)
                    mContext.getSystemService(Context.NOTIFICATION_SERVICE);
            manager.cancel(References.notification_id);
        }

        @Override
        protected void onPostExecute(String result) {
            mProgressDialog.cancel();
            Toast toast = Toast.makeText(mContext, mContext.getString(R.string.clean_completion),
                    Toast.LENGTH_LONG);
            toast.show();
            currentObject.getActivity().recreate();
        }

        @Override
        protected String doInBackground(String... sUrl) {
            // Uninstall theme
            References.uninstallOverlay(mContext, currentObject.getThemePackage());

            // Get all installed overlays for this package
            List<String> stateAll = ReadOverlays.main(4, mContext);
            stateAll.addAll(ReadOverlays.main(5, mContext));

            ArrayList<String> all_overlays = new ArrayList<>();
            for (int j = 0; j < stateAll.size(); j++) {
                try {
                    String current = stateAll.get(j);
                    ApplicationInfo appInfo = mContext.getPackageManager().getApplicationInfo(
                            current, PackageManager.GET_META_DATA);
                    if (appInfo.metaData != null && appInfo.metaData.getString(
                            "Substratum_Parent") != null) {
                        String parent = appInfo.metaData.getString("Substratum_Parent");
                        if (parent != null && parent.equals(currentObject.getThemePackage())) {
                            all_overlays.add(current);
                        }
                    }
                } catch (Exception e) {
                    // NameNotFound
                }
            }

            // Uninstall all overlays for this package
            References.uninstallOverlay(mContext, all_overlays);
            return null;
        }
    }

    private class deleteCache extends AsyncTask<String, Integer, String> {
        @Override
        protected void onPreExecute() {
            mProgressDialog = new ProgressDialog(mContext);
            mProgressDialog.setMessage(
                    mContext.getString(R.string.substratum_cache_clear_initial_toast));
            mProgressDialog.setIndeterminate(true);
            mProgressDialog.setCancelable(false);
            mProgressDialog.show();
            References.clearAllNotifications(mContext);
        }

        @Override
        protected void onPostExecute(String result) {
            // Since the cache is invalidated, better relaunch the app now
            mProgressDialog.cancel();
            mActivity.finish();
            mContext.startActivity(mActivity.getIntent());
        }

        @Override
        protected String doInBackground(String... sUrl) {
            // Delete the directory
            try {
                References.delete(mContext, mContext.getCacheDir().getAbsolutePath() +
                        "/SubstratumBuilder/");
            } catch (Exception e) {
                // Suppress warning
            }
            // Reset the flag for is_updating
            SharedPreferences prefsPrivate = mContext.getSharedPreferences("substratum_state",
                            Context.MODE_PRIVATE);
            prefsPrivate.edit().remove("is_updating").apply();
            return null;
        }
    }
}