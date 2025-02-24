package com.example.a1;

import android.app.DatePickerDialog;
import android.app.TimePickerDialog;
import android.os.Bundle;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.ListView;
import android.widget.SearchView;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class UserActivity extends AppCompatActivity {
    private ListView servicesListView;
    private Button bookServiceButton;
    private FirebaseFirestore db;
    private List<Service> serviceList;
    private List<Service> originalServiceList;
    private ServiceAdapter adapter;
    private Calendar calendar;
    private TextView dataField, timeField;

    private SearchView searchViewServices;
    private Spinner spinnerServiceCategories;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_user);

        db = FirebaseFirestore.getInstance();
        servicesListView = findViewById(R.id.servicesListView);
        bookServiceButton = findViewById(R.id.bookServiceButton);
        searchViewServices = findViewById(R.id.searchViewServices);
        spinnerServiceCategories = findViewById(R.id.spinnerServiceCategories);

        serviceList = new ArrayList<>();
        originalServiceList = new ArrayList<>();
        adapter = new ServiceAdapter(this, serviceList);
        servicesListView.setAdapter(adapter);
        servicesListView.setChoiceMode(ListView.CHOICE_MODE_SINGLE);

        calendar = Calendar.getInstance();

        bookServiceButton.setOnClickListener(v -> {
            int selectedPosition = servicesListView.getCheckedItemPosition();
            if (selectedPosition != ListView.INVALID_POSITION) {
                Service selectedService = serviceList.get(selectedPosition);
                showDateTimePickerDialog(selectedService.getServiceId());
            } else {
                Toast.makeText(this, "Выберите услугу", Toast.LENGTH_SHORT).show();
            }
        });

        loadServices();

        searchViewServices.setOnQueryTextListener(new SearchView.OnQueryTextListener() {
            @Override
            public boolean onQueryTextSubmit(String query) {
                return false;
            }

            @Override
            public boolean onQueryTextChange(String newText) {
                if (newText.isEmpty()) {
                    restoreFullServiceList();
                } else {
                    filterServices(newText);
                }
                return true;
            }
        });


        setupCategorySpinner();
    }

    private void loadServices() {
        db.collection("services")
                .get()
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful()) {
                        serviceList.clear();
                        originalServiceList.clear();

                        for (QueryDocumentSnapshot document : task.getResult()) {
                            String serviceId = document.getId();
                            String serviceName = document.getString("serviceName");
                            double price = document.getDouble("price");
                            String category = document.getString("category");

                            Service service = new Service(serviceId, serviceName, price, category);
                            serviceList.add(service);
                            originalServiceList.add(service);
                        }
                        adapter.notifyDataSetChanged();
                    } else {
                        Toast.makeText(this, "Ошибка при загрузке услуг", Toast.LENGTH_SHORT).show();
                    }
                });
    }

    private void filterServices(String query) {
        List<Service> filteredList = new ArrayList<>();

        for (Service service : originalServiceList) {
            if (service.getServiceName().toLowerCase().contains(query.toLowerCase())) {
                filteredList.add(service);
            }
        }
        updateAdapter(filteredList);
    }

    private void setupCategorySpinner() {
        String[] categories = {"All", "Hair", "Nails", "Spa Services"};
        ArrayAdapter<String> categoryAdapter = new ArrayAdapter<>(this,
                android.R.layout.simple_spinner_item, categories);
        categoryAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinnerServiceCategories.setAdapter(categoryAdapter);
        spinnerServiceCategories.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                String selectedCategory = categories[position];
                filterServicesByCategory(selectedCategory);
            }
            @Override
            public void onNothingSelected(AdapterView<?> parent) {
            }
        });
    }

    private void filterServicesByCategory(String category) {
        List<Service> filteredList = new ArrayList<>();
        if (category.equals("All")) {
            filteredList.addAll(originalServiceList);
        } else {
            for (Service service : originalServiceList) {
                if (service.getCategory() != null && service.getCategory().equals(category)) {
                    filteredList.add(service);
                }
            }
        }
        updateAdapter(filteredList);
    }

    private void restoreFullServiceList() {
        updateAdapter(originalServiceList);
    }
    private void updateAdapter(List<Service> newList) {
        serviceList.clear();
        serviceList.addAll(newList);
        adapter.notifyDataSetChanged();
    }
    private void showDateTimePickerDialog(String servicesID) {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("Выберите дату и время для записи");

        View view = getLayoutInflater().inflate(R.layout.dialog_date_time_picker, null);
        builder.setView(view);

        dataField = view.findViewById(R.id.dateField);
        timeField = view.findViewById(R.id.timeField);

        dataField.setOnClickListener(v -> showDatePickerDialog());
        timeField.setOnClickListener(v -> showTimePickerDialog());

        builder.setPositiveButton("Записаться", (dialog, which) -> {
            String date = dataField.getText().toString().trim();
            String time = timeField.getText().toString().trim();
            if (date.isEmpty() || time.isEmpty()) {
                Toast.makeText(UserActivity.this, "Выберите дату и время", Toast.LENGTH_SHORT).show();
                return;
            }
            bookService(servicesID, date, time);
        });
        builder.setNegativeButton("Отмена", (dialog, which) -> dialog.dismiss());
        builder.create().show();
    }

    private void bookService(String servicesID, String date, String time) {
        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
        if (user != null) {
            String clientName = user.getEmail();
            db.collection("services").document(servicesID).get()
                    .addOnSuccessListener(documentSnapshot -> {
                        if (documentSnapshot.exists()) {
                            String servicename = documentSnapshot.getString("serviceName");
                            Map<String, Object> appointmnet = new HashMap<>();
                            appointmnet.put("clientId", user.getUid());
                            appointmnet.put("clientName", clientName);
                            appointmnet.put("serviceId", servicesID);
                            appointmnet.put("serviceName", servicename);
                            appointmnet.put("date", date);
                            appointmnet.put("time", time);
                            db.collection("appointments").add(appointmnet).addOnSuccessListener(documentReference -> {
                                Toast.makeText(UserActivity.this, "Запись создана", Toast.LENGTH_SHORT).show();
                            }).addOnFailureListener(e -> {
                                Toast.makeText(UserActivity.this, "Ошибка", Toast.LENGTH_SHORT).show();
                            });
                        } else {
                            Toast.makeText(UserActivity.this, "Запись создана", Toast.LENGTH_SHORT).show();
                        }
                    });
        }
    }

    private void showTimePickerDialog() {
        TimePickerDialog timePickerDialog = new TimePickerDialog(this, (view, hourOfDay, minute) -> {
            calendar.set(Calendar.HOUR_OF_DAY, hourOfDay);
            calendar.set(Calendar.MINUTE, minute);

            timeField.setText(String.format("%02d:%02d", hourOfDay, minute));
        },
                calendar.get(Calendar.HOUR_OF_DAY),
                calendar.get(Calendar.MINUTE),
                true);
        timePickerDialog.show();
    }

    private void showDatePickerDialog() {
        DatePickerDialog datePickerDialog = new DatePickerDialog(this, (view, year, month, dayOfMonth) -> {
            calendar.set(Calendar.YEAR, year);
            calendar.set(Calendar.MONTH, month);
            calendar.set(Calendar.DAY_OF_MONTH, dayOfMonth);

            dataField.setText(String.format("%02d-%02d-%d", dayOfMonth, month + 1, year));
        },
                calendar.get(Calendar.YEAR),
                calendar.get(Calendar.MONTH),
                calendar.get(Calendar.DAY_OF_MONTH));
        datePickerDialog.getDatePicker().setMinDate(System.currentTimeMillis() - 1000);
        datePickerDialog.show();
    }
}
