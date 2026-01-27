import pandas as pd
import os

def book_csv_to_json(csv_file_path, json_file_path):
    # Read the CSV file into a DataFrame
    df = pd.read_csv(csv_file_path, encoding='latin-1', sep=";")

    # Convert the DataFrame to a JSON string

    selected_columns = ['ISBN', 'Book-Title', 'Book-Author', 'Year-Of-Publication','Publisher']
    df_subset = df[selected_columns]

    # Rename columns to match desired JSON keys
    df_subset = df_subset.rename(columns={
         'ISBN': 'isbn',
         'Book-Title': 'title',
         'Book-Author': 'author',
         'Year-Of-Publication': 'year',
         'Publisher': 'Publisher'})

    json_str = df_subset.to_json(orient='records', indent=4)

    # Write the JSON string to a file
    with open(json_file_path, 'w') as json_f:
        json_f.write(json_str)


if __name__ == "__main__":

    script_dir = os.path.dirname(os.path.abspath(__file__))
    csv_file = os.path.join(script_dir, "..\\src\\main\\resources\\BX_Books.csv")
    json_file = os.path.join(script_dir, 'JSON', 'books.json')
    book_csv_to_json(csv_file, json_file)
