import pandas as pd
import os

def users_csv_to_json(csv_file_path, json_file_path):


    df = pd.read_csv(csv_file_path, encoding='latin-1', sep=";")


    # Rename columns to match desired JSON keys
    df = df.rename(columns={
        'User-ID': 'id',
        'Location': 'country',
        'Age': 'Age'})

    # Extract country from all location
    df['country'] = df['country'].str.split(',').str[-1].str.strip().str.upper()

    # @todo From another DS catch the profile names
    df['name'] = "ABC"
    df['BAN_FLAG'] = False

    # @todo create the connection with books and reviews and populate these lists
    df['liked_books'] = df['id'].apply(lambda x: [])
    df['toread_list'] = df['id'].apply(lambda x: [])
    df['liked_reviews'] = df['id'].apply(lambda x: [])
    df['personal_lists'] = df['id'].apply(lambda x: [])
    df['friends'] = df['id'].apply(lambda x: [])
    df['followers'] = df['id'].apply(lambda x: [])



    json_str = df.to_json(orient='records', indent=4)

    # Write the JSON string to a file
    with open(json_file_path, 'w') as json_f:
        json_f.write(json_str)


if __name__ == "__main__":

    script_dir = os.path.dirname(os.path.abspath(__file__))
    csv_file = os.path.join(script_dir, "..\\src\\main\\resources\\BX-Users.csv")
    json_file = os.path.join(script_dir, 'JSON', 'users.json')
    users_csv_to_json(csv_file, json_file)
