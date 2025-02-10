package com.example.to_do_app;

import android.app.AlertDialog;
import android.content.Context;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Spinner;
import android.widget.Toast;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.List;

public class MainActivity extends AppCompatActivity {

    private EditText taskInput;
    private Spinner statusSpinner;
    private Button addTaskButton;
    private RecyclerView recyclerView;
    private TaskAdapter taskAdapter;
    private List<Task> taskList;
    private SharedPreferences sharedPreferences;

    private static final String PREFS_NAME = "TaskPrefs";
    private static final String TASK_LIST_KEY = "TaskList";

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        taskInput = findViewById(R.id.taskInput);
        statusSpinner = findViewById(R.id.statusSpinner);
        addTaskButton = findViewById(R.id.addTaskButton);
        recyclerView = findViewById(R.id.recyclerView);

        sharedPreferences = getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);

        // Spinner Setup
        ArrayAdapter<CharSequence> statusAdapter = ArrayAdapter.createFromResource(this,
                R.array.task_status_array, android.R.layout.simple_spinner_item);
        statusAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        statusSpinner.setAdapter(statusAdapter);

        taskList = new ArrayList<>();
        loadTasks(); // Load saved tasks

        taskAdapter = new TaskAdapter(taskList, new TaskAdapter.OnTaskClickListener() {
            @Override
            public void onEdit(int position) {
                showEditTaskDialog(position);
            }

            @Override
            public void onDelete(int position) {
                taskList.remove(position);
                taskAdapter.notifyItemRemoved(position);
                saveTasks();
            }
        });

        recyclerView.setLayoutManager(new LinearLayoutManager(this));
        recyclerView.setAdapter(taskAdapter);

        addTaskButton.setOnClickListener(v -> {
            String taskTitle = taskInput.getText().toString();
            String taskStatus = statusSpinner.getSelectedItem().toString();

            if (taskTitle.isEmpty()) {
                Toast.makeText(MainActivity.this, "Please enter a task", Toast.LENGTH_SHORT).show();
            } else {
                taskList.add(new Task(taskTitle, taskStatus));
                taskAdapter.notifyItemInserted(taskList.size() - 1);
                saveTasks();
                taskInput.setText("");  // Clear input field
            }
        });
    }

    private void showEditTaskDialog(int position) {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("Edit Task");

        View customView = getLayoutInflater().inflate(R.layout.dialog_edit_task, null);
        EditText editTaskInput = customView.findViewById(R.id.editTaskInput);
        Spinner editStatusSpinner = customView.findViewById(R.id.editStatusSpinner);

        editTaskInput.setText(taskList.get(position).getTitle());
        ArrayAdapter<CharSequence> statusAdapter = ArrayAdapter.createFromResource(this,
                R.array.task_status_array, android.R.layout.simple_spinner_item);
        statusAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        editStatusSpinner.setAdapter(statusAdapter);

        String currentStatus = taskList.get(position).getStatus();
        int statusPosition = statusAdapter.getPosition(currentStatus);
        editStatusSpinner.setSelection(statusPosition);

        builder.setView(customView);
        builder.setPositiveButton("Save", (dialog, which) -> {
            String newTitle = editTaskInput.getText().toString();
            String newStatus = editStatusSpinner.getSelectedItem().toString();

            if (!newTitle.isEmpty()) {
                taskList.get(position).setTitle(newTitle);
                taskList.get(position).setStatus(newStatus);
                taskAdapter.notifyItemChanged(position);
                saveTasks();
            } else {
                Toast.makeText(MainActivity.this, "Task cannot be empty", Toast.LENGTH_SHORT).show();
            }
        });

        builder.setNegativeButton("Cancel", (dialog, which) -> dialog.dismiss());
        builder.show();
    }

    private void saveTasks() {
        SharedPreferences.Editor editor = sharedPreferences.edit();
        JSONArray jsonArray = new JSONArray();

        for (Task task : taskList) {
            JSONObject taskObject = new JSONObject();
            try {
                taskObject.put("title", task.getTitle());
                taskObject.put("status", task.getStatus());
                jsonArray.put(taskObject);
            } catch (JSONException e) {
                e.printStackTrace();
            }
        }

        editor.putString(TASK_LIST_KEY, jsonArray.toString());
        editor.apply();
    }

    private void loadTasks() {
        String taskJson = sharedPreferences.getString(TASK_LIST_KEY, null);
        if (taskJson != null) {
            try {
                JSONArray jsonArray = new JSONArray(taskJson);
                for (int i = 0; i < jsonArray.length(); i++) {
                    JSONObject taskObject = jsonArray.getJSONObject(i);
                    String title = taskObject.getString("title");
                    String status = taskObject.getString("status");
                    taskList.add(new Task(title, status));
                }
            } catch (JSONException e) {
                e.printStackTrace();
            }
        }
    }
}
