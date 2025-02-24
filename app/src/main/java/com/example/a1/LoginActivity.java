package com.example.a1;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.util.Patterns;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;

import com.google.firebase.Firebase;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.FirebaseFirestore;

import java.nio.Buffer;
import java.util.HashMap;
import java.util.Map;

public class LoginActivity extends AppCompatActivity {
private EditText emailField, passwordField;
private Button loginButton, registrationButton;
private FirebaseAuth auth;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.auth_activity);

        auth = FirebaseAuth.getInstance();
        emailField = findViewById(R.id.emailField);
        passwordField = findViewById(R.id.passwordField);
        loginButton = findViewById(R.id.loginButton);
        registrationButton = findViewById(R.id.registerButton);

        loginButton.setOnClickListener(v -> loginUser());
        registrationButton.setOnClickListener(v->registrUser());

    }

    private void registrUser() {
        String email= emailField.getText().toString().trim();
        String password= passwordField.getText().toString().trim();
        if (email.isEmpty() || password.isEmpty()){
            Toast.makeText(LoginActivity.this,"Заполните все поля для входа",Toast.LENGTH_SHORT).show();
            return;
        }
        if(!Patterns.EMAIL_ADDRESS.matcher(email).matches()){
            Toast.makeText(LoginActivity.this,"Неправильный пароль",Toast.LENGTH_SHORT).show();
            return;
        }
        if(password.length()<6){
            Toast.makeText(LoginActivity.this,"Пароль не должен быть меньше 6 символом",Toast.LENGTH_SHORT).show();
            return;
        }
        auth.createUserWithEmailAndPassword(email,password).addOnCompleteListener(this,task -> {
            if(task.isSuccessful()){
                Toast.makeText(LoginActivity.this,"Регистрация прошла успешно",Toast.LENGTH_SHORT).show();
                saveUserFirestone(email);
            }
            else{
                if(task.getException() != null){
                    String errorMessage =task.getException().getMessage();
                    Toast.makeText(LoginActivity.this,errorMessage,Toast.LENGTH_SHORT).show();
                    Log.e("AuthError", "Ошибка регистрации", task.getException());
                }
            }
        });
    }

    private void loginUser() {
        String email= emailField.getText().toString().trim();
        String password= passwordField.getText().toString().trim();
        auth.signInWithEmailAndPassword(email,password).addOnCompleteListener(this, task -> {
            if(task.isSuccessful()){
                checkUserRole();
            }
            else{
                Toast.makeText(LoginActivity.this, "Ошибка авторизации",Toast.LENGTH_SHORT).show();
            }
        });

    }

    private void saveUserFirestone(String email) {
        FirebaseFirestore db = FirebaseFirestore.getInstance();
        Map<String,Object> user = new HashMap<>();
        user.put("email", email);
        user.put("role", "user");
        db.collection("users").document(auth.getCurrentUser().getUid()).set(user).addOnSuccessListener(a->{
            Toast.makeText(LoginActivity.this,"Данные сохренены",Toast.LENGTH_SHORT).show();
            startActivity(new Intent(LoginActivity.this, MainActivity.class));
            finish();
        })
        .addOnFailureListener(e ->{
            Toast.makeText(LoginActivity.this,"Данные не сохренены" + e.getMessage(),Toast.LENGTH_SHORT).show();
        });
    }
    private void checkUserRole() {
        FirebaseUser user = auth.getCurrentUser();
        if (user != null) {
            FirebaseFirestore db = FirebaseFirestore.getInstance();
            db.collection("users").document(user.getUid()).get()
                    .addOnSuccessListener(documentSnapshot -> {
                        String role = documentSnapshot.getString("role");
                        if (role != null) {
                            switch (role) {
                                case "admin":
                                    startActivity(new Intent(LoginActivity.this, AdminActivity.class));
                                    break;
                                case "employee":
                                    startActivity(new Intent(LoginActivity.this, EmployeeActivity.class));
                                    break;
                                case "user":
                                    startActivity(new Intent(LoginActivity.this, UserActivity.class));
                                    break;
                            }
                            finish();
                        }
                    });
        }
    }


}
