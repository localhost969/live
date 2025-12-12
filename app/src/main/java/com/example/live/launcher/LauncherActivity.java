package com.example.live.launcher;

import android.content.ActivityNotFoundException;
import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.TextView;
import android.widget.NumberPicker;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import androidx.viewpager2.widget.ViewPager2;

import com.example.live.R;
import com.google.android.material.textfield.TextInputEditText;
import com.google.android.material.textfield.TextInputLayout;

import java.text.SimpleDateFormat;
import java.text.DateFormatSymbols;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

public final class LauncherActivity extends AppCompatActivity {

    private static final int REQ_PERMS = 1201;
    private static final long WEATHER_MIN_INTERVAL_MS = 30L * 60L * 1000L;

    private AppsAdapter adapter;

    private ViewPager2 pager;

    // Home page views
    private TextView weatherLine;
    private QuickContactsAdapter quickContactsAdapter;
    private TextView addQuickContactBtn;
    private RecyclerView quickContactsList;

    // Calendar page views
    private TextView monthLabel;
    private TextView upcomingHolidays;
    private TextView dayDetails;
    private MonthGridAdapter monthAdapter;
    private final Calendar visibleMonth = Calendar.getInstance();

    private static final String HOLIDAYS_COUNTRY_CODE = "IN";

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        if (!PasswordManager.isPasswordSet(this)) {
            startActivity(new Intent(this, SetupActivity.class));
            finish();
            return;
        }

        // Extended to edges behind status bar
        getWindow().setStatusBarColor(0xFFFFFFFF);
        getWindow().getDecorView().setSystemUiVisibility(View.SYSTEM_UI_FLAG_LAYOUT_STABLE | View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN);

        setContentView(R.layout.activity_launcher);

        pager = findViewById(R.id.launcher_pager);
        pager.setOffscreenPageLimit(1);

        LauncherPagerAdapter pagerAdapter = new LauncherPagerAdapter(new LauncherPagerAdapter.PageBoundListener() {
            @Override
            public void onCalendarPageBound(View calendarRoot) {
                bindCalendarPage(calendarRoot);
            }

            @Override
            public void onHomePageBound(View homeRoot) {
                bindHomePage(homeRoot);
            }

            @Override
            public void onOthersPageBound(View othersRoot) {
                bindOthersPage(othersRoot);
            }
        });

        pager.setAdapter(pagerAdapter);
        // Calendar is index 0, Home is index 1, Others is index 2; start on Home.
        pager.setCurrentItem(LauncherPagerAdapter.PAGE_HOME, false);

        pager.registerOnPageChangeCallback(new ViewPager2.OnPageChangeCallback() {
            @Override
            public void onPageSelected(int position) {
                if (position == LauncherPagerAdapter.PAGE_CALENDAR) {
                    refreshCalendar();
                }
            }
        });
    }

    @Override
    protected void onResume() {
        super.onResume();
        renderPinnedApps();
        refreshWeather();
        renderQuickContacts();
        refreshCalendar();
    }

    private void bindHomePage(View root) {
        weatherLine = root.findViewById(R.id.weather_line);

        if (weatherLine != null) {
            weatherLine.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    promptSetLocation();
                }
            });
        }

        // Top labels for essentials: make them actionable (launch corresponding apps)
        View labelPhone = root.findViewById(R.id.focus_label_phone);
        if (labelPhone != null) {
            labelPhone.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    launchEssential(LauncherEssential.PHONE);
                }
            });
        }

        View labelMessages = root.findViewById(R.id.focus_label_messages);
        if (labelMessages != null) {
            labelMessages.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    launchEssential(LauncherEssential.MESSAGES);
                }
            });
        }

        View labelGmail = root.findViewById(R.id.focus_label_gmail);
        if (labelGmail != null) {
            labelGmail.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    launchEssential(LauncherEssential.GMAIL);
                }
            });
        }

        // Quick contacts
        quickContactsList = root.findViewById(R.id.quick_contacts_list);
        quickContactsAdapter = new QuickContactsAdapter(this, contacts -> {
            QuickContactsStore.updatePositions(LauncherActivity.this, contacts);
        });
        quickContactsList.setLayoutManager(new LinearLayoutManager(
                this, LinearLayoutManager.HORIZONTAL, false));
        quickContactsList.setAdapter(quickContactsAdapter);
        quickContactsAdapter.attachToRecyclerView(quickContactsList);
        
        // Exit edit mode when clicking outside
        root.setOnClickListener(v -> {
            if (quickContactsAdapter != null) {
                quickContactsAdapter.exitEditMode();
            }
        });
        quickContactsList.setOnClickListener(v -> {
            // Prevent propagation
        });

        addQuickContactBtn = root.findViewById(R.id.add_quick_contact);
        if (addQuickContactBtn != null) {
            addQuickContactBtn.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    promptAddQuickContact();
                }
            });
        }

        // Initial render
        refreshWeather();
        renderQuickContacts();
    }

    private void bindOthersPage(View root) {
        RecyclerView rv = root.findViewById(R.id.pinned_list);
        rv.setLayoutManager(new GridLayoutManager(this, 2));

        adapter = new AppsAdapter(new AppsAdapter.Listener() {
            @Override
            public void onAppClicked(AppInfo app) {
                Intent launch = AppRepository.getLaunchIntent(LauncherActivity.this, app.packageName);
                if (launch != null) startActivity(launch);
            }

            @Override
            public void onAppLongPressed(AppInfo app) {
                // no-op (keep minimalist)
            }
        }, false);
        rv.setAdapter(adapter);

        root.findViewById(R.id.open_all_apps).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                promptPasswordAndOpenAllApps();
            }
        });

        root.findViewById(R.id.open_setup).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                promptPasswordAndOpenSetup();
            }
        });

        // Initial render
        renderPinnedApps();
    }

    private void bindCalendarPage(View root) {
        monthLabel = root.findViewById(R.id.month_label);
        upcomingHolidays = root.findViewById(R.id.upcoming_holidays);
        dayDetails = root.findViewById(R.id.day_details);

        RecyclerView grid = root.findViewById(R.id.month_grid);
        grid.setLayoutManager(new GridLayoutManager(this, 7));
        monthAdapter = new MonthGridAdapter(new MonthGridAdapter.Listener() {
            @Override
            public void onDayClicked(MonthGridAdapter.DayCell day) {
                showDayDetails(day.dayStartUtcMs);
            }
        });
        grid.setAdapter(monthAdapter);

        root.findViewById(R.id.month_prev).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                visibleMonth.add(Calendar.MONTH, -1);
                refreshCalendar();
            }
        });
        root.findViewById(R.id.month_next).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                visibleMonth.add(Calendar.MONTH, 1);
                refreshCalendar();
            }
        });

        if (monthLabel != null) {
            monthLabel.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    showMonthYearPicker();
                }
            });
        }

        refreshCalendar();
    }

    private void refreshWeather() {
        if (weatherLine == null) return;

        WeatherClient.refreshIfStale(this, WEATHER_MIN_INTERVAL_MS, new WeatherClient.Callback() {
            @Override
            public void onResult(WeatherClient.WeatherResult result) {
                if (weatherLine != null) weatherLine.setText(result.displayLine);
            }
        });
    }

    private void promptSetLocation() {
        final View content = getLayoutInflater().inflate(R.layout.dialog_location_search, null);
        final TextInputLayout til = content.findViewById(R.id.location_layout);
        final TextInputEditText et = content.findViewById(R.id.location_input);

        final AlertDialog dialog = new AlertDialog.Builder(this)
                .setTitle(R.string.set_location_title)
                .setMessage(R.string.set_location_message)
                .setView(content)
                .setNegativeButton(android.R.string.cancel, null)
                .setPositiveButton(R.string.search, null)
                .show();

        dialog.getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                String q = et.getText() == null ? "" : et.getText().toString().trim();
                if (q.isEmpty()) {
                    til.setError(getString(R.string.location_required));
                    return;
                }
                til.setError(null);

                dialog.getButton(AlertDialog.BUTTON_POSITIVE).setEnabled(false);
                if (weatherLine != null) weatherLine.setText(getString(R.string.searching_location));

                WeatherClient.searchBestMatch(LauncherActivity.this, q, new WeatherClient.LocationSearchCallback() {
                    @Override
                    public void onResult(WeatherClient.LocationSearchResult result) {
                        dialog.getButton(AlertDialog.BUTTON_POSITIVE).setEnabled(true);
                        if (!result.ok || result.location == null) {
                            til.setError(result.errorMessage != null ? result.errorMessage : getString(R.string.location_search_failed));
                            refreshWeather();
                            return;
                        }

                        String label = result.location.label == null ? q : result.location.label;
                        WeatherClient.saveLocation(LauncherActivity.this, label, result.location.lat, result.location.lon);
                        refreshWeather();
                        dialog.dismiss();
                    }
                });
            }
        });
    }

    private void renderQuickContacts() {
        if (quickContactsAdapter == null) return;
        List<QuickContact> contacts = QuickContactsStore.getContacts(this);
        quickContactsAdapter.submitList(contacts);

        boolean empty = contacts.isEmpty();
        if (addQuickContactBtn != null) addQuickContactBtn.setVisibility(empty ? View.VISIBLE : View.GONE);
        if (quickContactsList != null) quickContactsList.setVisibility(empty ? View.GONE : View.VISIBLE);
    }

    private void promptAddQuickContact() {
        final View content = getLayoutInflater().inflate(R.layout.dialog_contact_number, null);
        final TextInputLayout nameLayout = content.findViewById(R.id.name_layout);
        final TextInputEditText nameInput = content.findViewById(R.id.name_input);
        final TextInputLayout numberLayout = content.findViewById(R.id.number_layout);
        final TextInputEditText numberInput = content.findViewById(R.id.number_input);

        final AlertDialog dialog = new AlertDialog.Builder(this)
                .setTitle(R.string.set_contact_title)
                .setMessage(R.string.set_contact_message)
                .setView(content)
                .setNegativeButton(android.R.string.cancel, null)
                .setPositiveButton(R.string.save_contact, null)
                .show();

        dialog.getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                String name = nameInput.getText() == null ? "" : nameInput.getText().toString().trim();
                String num = numberInput.getText() == null ? "" : numberInput.getText().toString().trim();
                
                if (name.isEmpty()) {
                    nameLayout.setError(getString(R.string.contact_name));
                    return;
                }
                nameLayout.setError(null);
                
                if (num.isEmpty()) {
                    numberLayout.setError(getString(R.string.phone_number));
                    return;
                }
                numberLayout.setError(null);
                
                QuickContactsStore.addContact(LauncherActivity.this, name, num);
                renderQuickContacts();
                dialog.dismiss();
            }
        });
    }

    private enum LauncherEssential {
        PHONE,
        MESSAGES,
        GMAIL
    }

    private void launchEssential(LauncherEssential essential) {
        Intent intent = null;
        try {
            if (essential == LauncherEssential.PHONE) {
                intent = new Intent(Intent.ACTION_DIAL);
            } else if (essential == LauncherEssential.MESSAGES) {
                intent = new Intent(Intent.ACTION_MAIN);
                intent.addCategory(Intent.CATEGORY_APP_MESSAGING);
            } else if (essential == LauncherEssential.GMAIL) {
                intent = AppRepository.getLaunchIntent(this, "com.google.android.gm");
                if (intent == null) {
                    intent = new Intent(Intent.ACTION_MAIN);
                    intent.addCategory(Intent.CATEGORY_APP_EMAIL);
                }
            }

            if (intent != null) startActivity(intent);
        } catch (ActivityNotFoundException anf) {
            // Ignore: keep it distraction-free.
        } catch (Throwable t) {
            // Ignore.
        }
    }

    private void renderPinnedApps() {
        // Home page may not yet be bound.
        if (adapter == null) return;
        List<AppInfo> all = AppRepository.getLaunchableApps(this);
        Set<String> pinned = PinnedAppsStore.getPinned(this);

        // Show only pinned packages that exist on this device.
        List<AppInfo> filtered = new ArrayList<>();
        for (AppInfo app : all) {
            if (pinned.contains(app.packageName)) filtered.add(app);
        }

        // If user pinned set is default and empty due to OEM package differences, fall back to showing nothing rather than adding noise.
        adapter.submit(filtered);

        TextView empty = getWindow().getDecorView().findViewById(R.id.empty_hint);
        if (empty != null) empty.setVisibility(filtered.isEmpty() ? View.VISIBLE : View.GONE);
    }

    private void refreshCalendar() {
        if (monthLabel == null || monthAdapter == null) return;

        Calendar month = (Calendar) visibleMonth.clone();
        month.set(Calendar.DAY_OF_MONTH, 1);
        setToStartOfDay(month);

        SimpleDateFormat fmt = new SimpleDateFormat("MMMM yyyy", Locale.getDefault());
        monthLabel.setText(fmt.format(month.getTime()));

        // Build 6-week grid (42 days), starting on Sunday.
        Calendar gridStart = (Calendar) month.clone();
        int firstDow = gridStart.get(Calendar.DAY_OF_WEEK);
        int offset = firstDow - Calendar.SUNDAY;
        if (offset < 0) offset += 7;
        gridStart.add(Calendar.DAY_OF_MONTH, -offset);

        int currentMonth = month.get(Calendar.MONTH);
        final int year = month.get(Calendar.YEAR);
        
        // Prefetch holidays for this year
        List<HolidaysClient.Holiday> holidays = HolidaysRepository.getPublicHolidaysBlocking(this, year, HOLIDAYS_COUNTRY_CODE);
        final Set<Integer> holidayKeys = new HashSet<>();
        for (HolidaysClient.Holiday h : holidays) {
            int k = dayKeyFromIso(h.dateIso);
            if (k != 0) holidayKeys.add(k);
        }

        List<MonthGridAdapter.DayCell> cells = new ArrayList<>(42);
        for (int i = 0; i < 42; i++) {
            Calendar c = (Calendar) gridStart.clone();
            c.add(Calendar.DAY_OF_MONTH, i);
            setToStartOfDay(c);
            boolean inMonth = c.get(Calendar.MONTH) == currentMonth;
            int dayKey = dayKey(c);
            int dow = c.get(Calendar.DAY_OF_WEEK);
            boolean isHol = holidayKeys.contains(dayKey);
            cells.add(new MonthGridAdapter.DayCell(c.getTimeInMillis(), dayKey, c.get(Calendar.DAY_OF_MONTH), inMonth, dow, isHol));
        }
        monthAdapter.submit(cells);

        // Holidays: show upcoming list (cached to storage so no refetch on every cold start).
        refreshHolidaysUi(year);

        // Default details: today.
        showDayDetails(System.currentTimeMillis());
    }

    private void showDayDetails(final long anyTimeMs) {
        if (dayDetails == null) return;
        final long start = startOfDay(anyTimeMs);

        dayDetails.setText("Loading…");

        new Thread(new Runnable() {
            @Override
            public void run() {
                String text = buildDayDetailsText(start, start + 24L * 60L * 60L * 1000L);
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        if (dayDetails != null) dayDetails.setText(text);
                    }
                });
            }
        }, "day-details").start();
    }

    private String buildDayDetailsText(long startMs, long endMs) {
        SimpleDateFormat iso = new SimpleDateFormat("yyyy-MM-dd", Locale.US);
        String dateIso = iso.format(startMs);

        // Holidays from local bundled list (2026 only)
        Calendar c = Calendar.getInstance();
        c.setTimeInMillis(startMs);
        int year = c.get(Calendar.YEAR);
        List<HolidaysClient.Holiday> holidays = HolidaysRepository.getPublicHolidaysBlocking(this, year, HOLIDAYS_COUNTRY_CODE);
        Set<String> holidayNamesSet = new HashSet<>();
        for (HolidaysClient.Holiday h : holidays) {
            if (dateIso.equals(h.dateIso)) holidayNamesSet.add(h.name);
        }
        List<String> holidayNames = new ArrayList<>(holidayNamesSet);

        SimpleDateFormat header = new SimpleDateFormat("EEE, MMM d", Locale.getDefault());
        StringBuilder sb = new StringBuilder();
        sb.append(header.format(startMs));

        if (!holidayNames.isEmpty()) {
            sb.append("\n\nHolidays:\n");
            for (String h : holidayNames) sb.append("• ").append(h).append("\n");
        } else {
            sb.append("\n\nNo holidays.");
        }

        return sb.toString().trim();
    }

    private void refreshHolidaysUi(final int year) {
        if (upcomingHolidays == null) return;

        new Thread(new Runnable() {
            @Override
            public void run() {
                List<HolidaysClient.Holiday> holidays = HolidaysRepository.getPublicHolidaysBlocking(
                        LauncherActivity.this.getApplicationContext(),
                        year,
                        HOLIDAYS_COUNTRY_CODE
                );

                final Calendar visibleMonthCopy = (Calendar) visibleMonth.clone();
                final String upcoming = buildUpcomingHolidaysText(holidays, visibleMonthCopy);

                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        if (upcomingHolidays != null) {
                            if (upcoming == null || upcoming.trim().isEmpty()) {
                                upcomingHolidays.setVisibility(View.GONE);
                            } else {
                                upcomingHolidays.setText(upcoming);
                                upcomingHolidays.setVisibility(View.VISIBLE);
                            }
                        }
                    }
                });
            }
        }, "holidays").start();
    }

    private String buildUpcomingHolidaysText(@NonNull List<HolidaysClient.Holiday> holidays, @NonNull Calendar visibleMonth) {
        if (holidays.isEmpty()) return "";

        final int todayKey = dayKey(Calendar.getInstance());
        final int visibleMonthValue = visibleMonth.get(Calendar.MONTH);
        final int visibleYear = visibleMonth.get(Calendar.YEAR);

        // Separate holidays into upcoming (from today onwards) and this month's holidays
        Map<Integer, Set<String>> upcomingByDay = new HashMap<>();
        Map<Integer, Set<String>> monthByDay = new HashMap<>();

        for (HolidaysClient.Holiday h : holidays) {
            int k = dayKeyFromIso(h.dateIso);
            if (k == 0) continue;
            
            int hYear = k / 10000;
            int hMonth = (k / 100) % 100;
            
            // Add to month holidays if it's in the visible month
            if (hYear == visibleYear && (hMonth - 1) == visibleMonthValue) {
                Set<String> names = monthByDay.get(k);
                if (names == null) {
                    names = new HashSet<>();
                    monthByDay.put(k, names);
                }
                names.add(h.name);
            }
            
            // Add to upcoming if it's from today onwards
            if (k >= todayKey) {
                Set<String> names = upcomingByDay.get(k);
                if (names == null) {
                    names = new HashSet<>();
                    upcomingByDay.put(k, names);
                }
                names.add(h.name);
            }
        }

        StringBuilder sb = new StringBuilder();

        // Build upcoming holidays section
        if (!upcomingByDay.isEmpty()) {
            List<Integer> keys = new ArrayList<>(upcomingByDay.keySet());
            Collections.sort(keys);

            sb.append("Upcoming holidays:\n");
            int shown = 0;
            for (int k : keys) {
                Set<String> namesSet = upcomingByDay.get(k);
                if (namesSet == null || namesSet.isEmpty()) continue;
                List<String> names = new ArrayList<>(namesSet);
                Collections.sort(names, new Comparator<String>() {
                    @Override
                    public int compare(String a, String b) {
                        if (a == null && b == null) return 0;
                        if (a == null) return 1;
                        if (b == null) return -1;
                        return a.compareToIgnoreCase(b);
                    }
                });

                sb.append("• ").append(formatDayKey(k)).append(" — ").append(joinNames(names)).append("\n");
                shown++;
                if (shown >= 5) break;
            }
        }

        // Build this month's holidays section
        if (!monthByDay.isEmpty()) {
            if (sb.length() > 0) sb.append("\n");
            sb.append("This month's holidays:\n");
            
            List<Integer> keys = new ArrayList<>(monthByDay.keySet());
            Collections.sort(keys);

            for (int k : keys) {
                Set<String> namesSet = monthByDay.get(k);
                if (namesSet == null || namesSet.isEmpty()) continue;
                List<String> names = new ArrayList<>(namesSet);
                Collections.sort(names, new Comparator<String>() {
                    @Override
                    public int compare(String a, String b) {
                        if (a == null && b == null) return 0;
                        if (a == null) return 1;
                        if (b == null) return -1;
                        return a.compareToIgnoreCase(b);
                    }
                });

                sb.append("• ").append(formatDayKey(k)).append(" — ").append(joinNames(names)).append("\n");
            }
        }

        return sb.toString().trim();
    }

    private static String joinNames(@NonNull List<String> names) {
        if (names.isEmpty()) return "Holiday";
        if (names.size() == 1) return names.get(0);
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < names.size(); i++) {
            if (i > 0) sb.append(", ");
            sb.append(names.get(i));
        }
        return sb.toString();
    }

    private static int dayKey(@NonNull Calendar c) {
        int y = c.get(Calendar.YEAR);
        int m = c.get(Calendar.MONTH) + 1;
        int d = c.get(Calendar.DAY_OF_MONTH);
        return (y * 10000) + (m * 100) + d;
    }

    private static int dayKeyFromIso(@Nullable String iso) {
        try {
            if (iso == null || iso.length() < 10) return 0;
            int y = Integer.parseInt(iso.substring(0, 4));
            int m = Integer.parseInt(iso.substring(5, 7));
            int d = Integer.parseInt(iso.substring(8, 10));
            return (y * 10000) + (m * 100) + d;
        } catch (Throwable t) {
            return 0;
        }
    }

    private static String formatDayKey(int dayKey) {
        try {
            int y = dayKey / 10000;
            int m = (dayKey / 100) % 100;
            int d = dayKey % 100;

            Calendar c = Calendar.getInstance();
            c.set(Calendar.YEAR, y);
            c.set(Calendar.MONTH, m - 1);
            c.set(Calendar.DAY_OF_MONTH, d);
            setToStartOfDay(c);

            SimpleDateFormat fmt = new SimpleDateFormat("MMM d", Locale.getDefault());
            return fmt.format(c.getTime());
        } catch (Throwable t) {
            return "";
        }
    }

    private void showMonthYearPicker() {
        if (monthLabel == null) return;

        final View content = getLayoutInflater().inflate(R.layout.dialog_month_year_picker, null);
        final NumberPicker monthPicker = content.findViewById(R.id.picker_month);
        final NumberPicker yearPicker = content.findViewById(R.id.picker_year);

        final String[] months = new DateFormatSymbols(Locale.getDefault()).getMonths();
        final String[] monthNames = new String[12];
        for (int i = 0; i < 12; i++) monthNames[i] = months[i];

        monthPicker.setMinValue(0);
        monthPicker.setMaxValue(11);
        monthPicker.setDisplayedValues(monthNames);
        monthPicker.setWrapSelectorWheel(true);

        int currentYear = visibleMonth.get(Calendar.YEAR);
        yearPicker.setMinValue(1970);
        yearPicker.setMaxValue(2100);
        yearPicker.setWrapSelectorWheel(false);

        monthPicker.setValue(visibleMonth.get(Calendar.MONTH));
        yearPicker.setValue(currentYear);

        new AlertDialog.Builder(this)
                .setTitle("Select month")
                .setView(content)
                .setNegativeButton(android.R.string.cancel, null)
                .setPositiveButton(android.R.string.ok, (d, w) -> {
                    visibleMonth.set(Calendar.YEAR, yearPicker.getValue());
                    visibleMonth.set(Calendar.MONTH, monthPicker.getValue());
                    visibleMonth.set(Calendar.DAY_OF_MONTH, 1);
                    refreshCalendar();
                })
                .show();
    }

    private     static void setToStartOfDay(Calendar c) {
        c.set(Calendar.HOUR_OF_DAY, 0);
        c.set(Calendar.MINUTE, 0);
        c.set(Calendar.SECOND, 0);
        c.set(Calendar.MILLISECOND, 0);
    }

    private static long startOfDay(long anyTimeMs) {
        Calendar c = Calendar.getInstance();
        c.setTimeInMillis(anyTimeMs);
        setToStartOfDay(c);
        return c.getTimeInMillis();
    }

    private void promptPasswordAndOpenAllApps() {
        showPasswordDialog(new Runnable() {
            @Override
            public void run() {
                startActivity(new Intent(LauncherActivity.this, AllAppsActivity.class));
            }
        });
    }

    private void promptPasswordAndOpenSetup() {
        showPasswordDialog(new Runnable() {
            @Override
            public void run() {
                startActivity(new Intent(LauncherActivity.this, SetupActivity.class));
            }
        });
    }

    private void showPasswordDialog(final Runnable onSuccess) {
        final View content = getLayoutInflater().inflate(R.layout.dialog_password, null);
        final TextInputLayout til = content.findViewById(R.id.password_layout);
        final TextInputEditText et = content.findViewById(R.id.password_input);

        new AlertDialog.Builder(this)
                .setTitle(R.string.unlock_title)
                .setView(content)
                .setNegativeButton(android.R.string.cancel, null)
                .setPositiveButton(R.string.unlock, null)
                .create();

        final AlertDialog dialog = new AlertDialog.Builder(this)
                .setTitle(R.string.unlock_title)
                .setView(content)
                .setNegativeButton(android.R.string.cancel, null)
                .setPositiveButton(R.string.unlock, null)
                .show();

        dialog.getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                String pwd = et.getText() == null ? "" : et.getText().toString();
                if (PasswordManager.verify(LauncherActivity.this, pwd)) {
                    dialog.dismiss();
                    if (onSuccess != null) onSuccess.run();
                } else {
                    til.setError(getString(R.string.wrong_password));
                }
            }
        });
    }
}
