import os

output_file = "project_code.txt"  # Output file name

# Define file extensions to include
extensions = (".java", ".html", ".css", ".js", ".sql", ".txt", ".md")

with open(output_file, "w", encoding="utf-8") as outfile:
    for root, _, files in os.walk("."):  # Walk through all files in project
        for file in files:
            if file.endswith(extensions):  # Check for required file types
                file_path = os.path.join(root, file)
                outfile.write(f"\n\n===== {file_path} =====\n\n")  # Add file name
                with open(file_path, "r", encoding="utf-8", errors="ignore") as infile:
                    outfile.write(infile.read())  # Append file content

print(f"✅ All code has been saved to {output_file}")
