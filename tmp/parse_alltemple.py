import pandas as pd
import json

try:
    df = pd.read_excel('d:/lead-declaration-system/backend/src/main/resources/templates/alltemple.xlsx')
    
    output = []
    output.append("Columns:")
    output.append(str(df.columns.tolist()))
    output.append("\nFirst few rows:")
    output.append(df.head().to_json(force_ascii=False, orient='records'))
    
    with open('d:/lead-declaration-system/tmp/out_alltemple.txt', 'w', encoding='utf-8') as f:
        f.write('\n'.join(output))
        
    print("Success")
except Exception as e:
    with open('d:/lead-declaration-system/tmp/out_alltemple.txt', 'w', encoding='utf-8') as f:
        f.write(f"Error parsing Excel: {e}")
    print("Error")
