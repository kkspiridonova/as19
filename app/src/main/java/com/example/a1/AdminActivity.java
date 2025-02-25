package com.example.a1;

import android.os.Bundle;
import android.util.Log;
import android.util.Patterns;
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

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;

import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

public class AdminActivity extends AppCompatActivity {
    private ListView servicesListView;
    private ListView usersListView;
    private FirebaseFirestore db;
    private List<Service> serviceList;
    private List<User> userList;
    private ServiceAdapter serviceAdapter;
    private UserAdapter userAdapter;
    private EditText serviceNameEditText, servicePriceEditText, serviceCategoryEditText;
    private EditText userEmailEditText, userRoleEditText;
    private Button createServiceButton, editServiceButton, deleteServiceButton;
    private Button createUserButton, editUserButton, deleteUserButton;
    private String selectedServiceId;
    private String selectedUserId;
    private SearchView searchViewServices;
    private Spinner spinnerServiceCategories;
    private ArrayAdapter<String> categoryAdapter;
    private List<String> categories;
    private List<Service> originalServiceList;

    private FirebaseAuth auth;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_admin);

        auth = FirebaseAuth.getInstance();

        db = FirebaseFirestore.getInstance();
        servicesListView = findViewById(R.id.servicesListView);
        usersListView = findViewById(R.id.usersListView);
        serviceNameEditText = findViewById(R.id.serviceNameEditText);
        servicePriceEditText = findViewById(R.id.servicePriceEditText);
        serviceCategoryEditText = findViewById(R.id.serviceCategoryEditText);
        userEmailEditText = findViewById(R.id.userEmailEditText);
        userRoleEditText = findViewById(R.id.userRoleEditText);
        createServiceButton = findViewById(R.id.createServiceButton);
        editServiceButton = findViewById(R.id.editServiceButton);
        deleteServiceButton = findViewById(R.id.deleteServiceButton);
        createUserButton = findViewById(R.id.createUserButton);
        editUserButton = findViewById(R.id.editUserButton);
        deleteUserButton = findViewById(R.id.deleteUserButton);
        searchViewServices = findViewById(R.id.searchViewServices);
        spinnerServiceCategories = findViewById(R.id.spinnerServiceCategories);

        serviceList = new ArrayList<>();
        originalServiceList = new ArrayList<>();
        userList = new ArrayList<>();
        serviceAdapter = new ServiceAdapter(this, serviceList);
        userAdapter = new UserAdapter(this, userList);

        servicesListView.setAdapter(serviceAdapter);
        usersListView.setAdapter(userAdapter);

        servicesListView.setChoiceMode(ListView.CHOICE_MODE_SINGLE);
        usersListView.setChoiceMode(ListView.CHOICE_MODE_SINGLE);

        categories = new ArrayList<>();
        categories.add("All");
        categories.add("Hair");
        categories.add("Nails");
        categories.add("Spa Services");

        categoryAdapter = new ArrayAdapter<>(this, android.R.layout.simple_spinner_dropdown_item, categories);
        spinnerServiceCategories.setAdapter(categoryAdapter);

        loadServices();
        loadUsers();

        createServiceButton.setOnClickListener(v -> createService());
        editServiceButton.setOnClickListener(v -> editService());
        deleteServiceButton.setOnClickListener(v -> deleteService());
        createUserButton.setOnClickListener(v -> createUser());
        editUserButton.setOnClickListener(v -> editUser());
        deleteUserButton.setOnClickListener(v -> deleteUser());

        servicesListView.setOnItemClickListener((parent, view, position, id) -> {
            Service selectedService = serviceList.get(position);
            selectedServiceId = selectedService.getServiceId();
            serviceNameEditText.setText(selectedService.getServiceName());
            servicePriceEditText.setText(String.valueOf(selectedService.getPrice()));
            serviceCategoryEditText.setText(selectedService.getCategory());
        });

        usersListView.setOnItemClickListener((parent, view, position, id) -> {
            User selectedUser = userList.get(position);
            selectedUserId = selectedUser.getUserId();
            userEmailEditText.setText(selectedUser.getEmail());
            userRoleEditText.setText(selectedUser.getRole());
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

        spinnerServiceCategories.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                String selectedCategory = categories.get(position);
                filterServicesByCategory(selectedCategory);
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {
            }
        });
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
                        serviceAdapter.notifyDataSetChanged();
                    } else {
                        Toast.makeText(this, "Ошибка при загрузке услуг", Toast.LENGTH_SHORT).show();
                    }
                });
    }

    private void createService() {
        String serviceName = serviceNameEditText.getText().toString().trim();
        String priceStr = servicePriceEditText.getText().toString().trim();
        String serviceCategory = serviceCategoryEditText.getText().toString().trim();

        if (serviceName.isEmpty() || priceStr.isEmpty()) {
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
                        String details = "Service Name: " + serviceName + ", Category: " + serviceCategory;
                        logAdminAction("create_service", details);
                        Log.d("AdminAction", "Created service: " + serviceName + ", category: " + serviceCategory);
                    })
                    .addOnFailureListener(e -> {
                        Toast.makeText(this, "Ошибка при создании услуги", Toast.LENGTH_SHORT).show();
                        Log.e("AdminAction", "Error creating service: " + serviceName, e);
                    });
        } catch (NumberFormatException e) {
            Toast.makeText(this, "Неверный формат цены", Toast.LENGTH_SHORT).show();
            Log.e("AdminAction", "Invalid price format for service: " + serviceName, e);
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

        if (serviceName.isEmpty() || priceStr.isEmpty()) {
            Toast.makeText(this, "Пожалуйста, заполните все поля для услуги", Toast.LENGTH_SHORT).show();
            return;
        }

        try {
            double price = Double.parseDouble(priceStr);
            db.collection("services")
                    .document(selectedServiceId)
                    .update("serviceName", serviceName, "price", price, "category", serviceCategory)
                    .addOnSuccessListener(aVoid -> {
                        Toast.makeText(this, "Услуга обновлена", Toast.LENGTH_SHORT).show();
                        clearServiceInputFields();
                        loadServices();
                        String details = "Service ID: " + selectedServiceId + ", Service Name: " + serviceName + ", Category: " + serviceCategory;
                        logAdminAction("edit_service", details);
                        Log.d("AdminAction", "Edited service: " + serviceName + ", category: " + serviceCategory);
                    })
                    .addOnFailureListener(e -> {
                        Toast.makeText(this, "Ошибка при обновлении услуги", Toast.LENGTH_SHORT).show();
                        Log.e("AdminAction", "Error editing service: " + serviceName, e);
                    });
        } catch (NumberFormatException e) {
            Toast.makeText(this, "Неверный формат цены", Toast.LENGTH_SHORT).show();
            Log.e("AdminAction", "Invalid price format for service: " + serviceName, e);
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
                    logAdminAction("delete_service", "Service ID: " + selectedServiceId);
                    Log.d("AdminAction", "Deleted service with ID: " + selectedServiceId);
                })
                .addOnFailureListener(e -> {
                    Toast.makeText(this, "Ошибка при удалении услуги", Toast.LENGTH_SHORT).show();
                    Log.e("AdminAction", "Error deleting service with ID: " + selectedServiceId, e);
                });
    }

    private void clearServiceInputFields() {
        serviceNameEditText.setText("");
        servicePriceEditText.setText("");
        serviceCategoryEditText.setText("");
    }

    private void loadUsers() {
        db.collection("users")
                .get()
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful()) {
                        userList.clear();
                        for (QueryDocumentSnapshot document : task.getResult()) {
                            String id = document.getId();
                            String email = document.getString("email");
                            String role = document.getString("role");
                            User user = new User(id, email, role);
                            userList.add(user);
                        }
                        userAdapter.notifyDataSetChanged();
                    } else {
                        Toast.makeText(this, "Ошибка при загрузке пользователей", Toast.LENGTH_SHORT).show();
                    }
                });
    }

    private void createUser() {
        String email = userEmailEditText.getText().toString().trim();
        String role = userRoleEditText.getText().toString().trim();

        if (email.isEmpty() || role.isEmpty()) {
            Toast.makeText(this, "Пожалуйста, заполните все поля для пользователя", Toast.LENGTH_SHORT).show();
            return;
        }
        if(!Patterns.EMAIL_ADDRESS.matcher(email).matches()){
            Toast.makeText(AdminActivity.this,"Неправильный email",Toast.LENGTH_SHORT).show();
            return;
        }

        User newUser = new User(null, email, role);

        db.collection("users")
                .add(newUser)
                .addOnSuccessListener(documentReference -> {
                    Toast.makeText(this, "Пользователь создан", Toast.LENGTH_SHORT).show();
                    clearUserInputFields();
                    loadUsers();
                    logAdminAction("create_user", "Email: " + email + ", Role: " + role);
                    Log.d("AdminAction", "Created user: " + email + ", role: " + role);
                })
                .addOnFailureListener(e -> {
                    Toast.makeText(this, "Ошибка при создании пользователя", Toast.LENGTH_SHORT).show();
                    Log.e("AdminAction", "Error creating user: " + email, e);
                });
    }

    private void editUser() {
        String email = userEmailEditText.getText().toString().trim();
        String role = userRoleEditText.getText().toString().trim();
        if (selectedUserId == null) {
            Toast.makeText(this, "Пожалуйста, выберите пользователя для редактирования", Toast.LENGTH_SHORT).show();
            return;
        }
        if (email.isEmpty() || role.isEmpty()) {
            Toast.makeText(this, "Пожалуйста, заполните все поля для пользователя", Toast.LENGTH_SHORT).show();
            return;
        }
        db.collection("users")
                .document(selectedUserId)
                .update("email", email, "role", role)
                .addOnSuccessListener(aVoid -> {
                    Toast.makeText(this, "Пользователь обновлен", Toast.LENGTH_SHORT).show();
                    clearUserInputFields();
                    loadUsers();
                    logAdminAction("edit_user", "User ID: " + selectedUserId + ", Email: " + email + ", Role: " + role);
                    Log.d("AdminAction", "Edited user: " + email + ", role: " + role);
                })
                .addOnFailureListener(e -> {
                    Toast.makeText(this, "Ошибка при обновлении пользователя", Toast.LENGTH_SHORT).show();
                    Log.e("AdminAction", "Error editing user: " + email, e);
                });
    }
    private void deleteUser() {
        if (selectedUserId == null) {
            Toast.makeText(this, "Пожалуйста, выберите пользователя для удаления", Toast.LENGTH_SHORT).show();
            return;
        }

        db.collection("users")
                .document(selectedUserId)
                .delete()
                .addOnSuccessListener(aVoid -> {
                    Toast.makeText(this, "Пользователь удален", Toast.LENGTH_SHORT).show();
                    clearUserInputFields();
                    loadUsers();
                    logAdminAction("delete_user", "User ID: " + selectedUserId);
                    Log.d("AdminAction", "Deleted user with ID: " + selectedUserId);
                })
                .addOnFailureListener(e -> {
                    Toast.makeText(this, "Ошибка при удалении пользователя", Toast.LENGTH_SHORT).show();
                    Log.e("AdminAction", "Error deleting user with ID: " + selectedUserId, e);
                });
    }

    private void clearUserInputFields() {
        userEmailEditText.setText("");
        userRoleEditText.setText("");
    }

    private void filterServices(String query) {
        List<Service> filteredList = new ArrayList<>();

        for (Service service : originalServiceList) {
            if (service.getServiceName().toLowerCase().contains(query.toLowerCase())) {
                filteredList.add(service);
            }
        }
        updateServiceAdapter(filteredList);
    }

    private void updateServiceAdapter(List<Service> newList) {
        serviceList.clear();
        serviceList.addAll(newList);
        serviceAdapter.notifyDataSetChanged();
    }

    private void filterServicesByCategory(String category) {
        List<Service> filteredList = new ArrayList<>();
        if (category.equals("All")) {
            filteredList.addAll(originalServiceList);
        } else {
            for (Service service : originalServiceList) {
                if (Objects.equals(service.getCategory(), category)) {
                    filteredList.add(service);
                }
            }
        }
        updateServiceAdapter(filteredList);
    }

    private void restoreFullServiceList() {
        updateServiceAdapter(originalServiceList);
    }

    private void logAdminAction(String action, String details) {
        FirebaseUser user = auth.getCurrentUser();
        if (user != null) {
            FirebaseFirestore db = FirebaseFirestore.getInstance();
            Map<String, Object> logEntry = new HashMap<>();
            logEntry.put("userId", user.getUid());
            logEntry.put("email", user.getEmail());
            logEntry.put("timestamp", new Date());
            logEntry.put("action", action);
            logEntry.put("details", details);

            db.collection("admin_actions")
                    .add(logEntry)
                    .addOnSuccessListener(documentReference -> {
                        Log.d("AdminActionLog", "Admin action logged: " + action + ", Details: " + details);
                    })
                    .addOnFailureListener(e -> {
                        Log.e("AdminActionLog", "Failed to log admin action: " + action, e);
                    });
        } else {
            Log.w("AdminActionLog", "Admin action could not be logged: no user.");
        }
    }
}
