package com.example.a1;

import android.os.Bundle;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ListView;
import android.widget.SearchView;
import android.widget.Spinner;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

public class EmployeeActivity extends AppCompatActivity {
    private ListView servicesListView;
    private FirebaseFirestore db;
    private List<Service> serviceList;
    private List<Service> originalServiceList;
    private ServiceAdapter adapter;
    private EditText serviceNameEditText, servicePriceEditText, serviceCategoryEditText;
    private Button createServiceButton, editServiceButton, deleteServiceButton;
    private String selectedServiceId;

    private SearchView searchViewServices;
    private Spinner spinnerServiceCategories;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_employee);

        db = FirebaseFirestore.getInstance();
        servicesListView = findViewById(R.id.servicesListView);
        serviceNameEditText = findViewById(R.id.serviceNameEditText);
        servicePriceEditText = findViewById(R.id.servicePriceEditText);
        serviceCategoryEditText = findViewById(R.id.serviceCategoryEditText);
        createServiceButton = findViewById(R.id.createServiceButton);
        editServiceButton = findViewById(R.id.editServiceButton);
        deleteServiceButton = findViewById(R.id.deleteServiceButton);

        searchViewServices = findViewById(R.id.searchViewServices);
        spinnerServiceCategories = findViewById(R.id.spinnerServiceCategories);

        serviceList = new ArrayList<>();
        originalServiceList = new ArrayList<>();
        adapter = new ServiceAdapter(this, serviceList);
        servicesListView.setAdapter(adapter);
        servicesListView.setChoiceMode(ListView.CHOICE_MODE_SINGLE);

        loadServices();

        createServiceButton.setOnClickListener(v -> createService());
        editServiceButton.setOnClickListener(v -> editService());
        deleteServiceButton.setOnClickListener(v -> deleteService());

        servicesListView.setOnItemClickListener((parent, view, position, id) -> {
            Service selectedService = serviceList.get(position);
            selectedServiceId = selectedService.getServiceId();
            serviceNameEditText.setText(selectedService.getServiceName());
            servicePriceEditText.setText(String.valueOf(selectedService.getPrice()));
            serviceCategoryEditText.setText(selectedService.getCategory());
        });

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
                            Double price = document.getDouble("price");
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
                String selectedCategory = Objects.requireNonNull(categories)[position];
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

    private void createService() {
        String serviceName = serviceNameEditText.getText().toString().trim();
        String priceStr = servicePriceEditText.getText().toString().trim();
        String serviceCategory = serviceCategoryEditText.getText().toString().trim();

        if (serviceName.isEmpty() || priceStr.isEmpty() || serviceCategory.isEmpty()) {
            Toast.makeText(this, "Пожалуйста, заполните все поля для услуги", Toast.LENGTH_SHORT).show();
            return;
        }

        try {
            double price = Double.parseDouble(priceStr);
            Service newService = new Service(null, serviceName, price, serviceCategory);

            db.collection("services")
                    .add(newService)
                    .addOnSuccessListener(documentReference -> {
                        Toast.makeText(this, "Услуга создана", Toast.LENGTH_SHORT).show();
                        clearServiceInputFields();
                        loadServices();
                    })
                    .addOnFailureListener(e -> {
                        Toast.makeText(this, "Ошибка при создании услуги", Toast.LENGTH_SHORT).show();
                    });
        } catch (NumberFormatException e) {
            Toast.makeText(this, "Неверный формат цены", Toast.LENGTH_SHORT).show();
        }
    }

    private void editService() {
        String serviceName = serviceNameEditText.getText().toString().trim();
        String priceStr = servicePriceEditText.getText().toString().trim();
        String serviceCategory = serviceCategoryEditText.getText().toString().trim();

        if (selectedServiceId == null) {
            Toast.makeText(this, "Пожалуйста, выберите услугу для редактирования", Toast.LENGTH_SHORT).show();
            return;
        }

        if (serviceName.isEmpty() || priceStr.isEmpty() || serviceCategory.isEmpty()) {
            Toast.makeText(this, "Пожалуйста, заполните все поля для услуги", Toast.LENGTH_SHORT).show();
            return;
        }

        try {
            double price = Double.parseDouble(priceStr);
            db.collection("services")
                    .document(selectedServiceId)
                    .update("serviceName", serviceName, "price", price, "category", serviceCategory) // Include category
                    .addOnSuccessListener(aVoid -> {
                        Toast.makeText(this, "Услуга обновлена", Toast.LENGTH_SHORT).show();
                        clearServiceInputFields();
                        loadServices();
                        selectedServiceId = null;
                    })
                    .addOnFailureListener(e -> {
                        Toast.makeText(this, "Ошибка при обновлении услуги", Toast.LENGTH_SHORT).show();
                    });
        } catch (NumberFormatException e) {
            Toast.makeText(this, "Неверный формат цены", Toast.LENGTH_SHORT).show();
        }
    }

    private void deleteService() {
        if (selectedServiceId == null) {
            Toast.makeText(this, "Пожалуйста, выберите услугу для удаления", Toast.LENGTH_SHORT).show();
            return;
        }

        db.collection("services")
                .document(selectedServiceId)
                .delete()
                .addOnSuccessListener(aVoid -> {
                    Toast.makeText(this, "Услуга удалена", Toast.LENGTH_SHORT).show();
                    clearServiceInputFields();
                    loadServices();
                    selectedServiceId = null;
                })
                .addOnFailureListener(e -> {
                    Toast.makeText(this, "Ошибка при удалении услуги", Toast.LENGTH_SHORT).show();
                });
    }

    private void clearServiceInputFields() {
        serviceNameEditText.setText("");
        servicePriceEditText.setText("");
        serviceCategoryEditText.setText("");
    }
}
