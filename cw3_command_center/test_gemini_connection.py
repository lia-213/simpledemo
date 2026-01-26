import google.generativeai as genai
import os

api_key = os.getenv('GEMINI_API_KEY')
if not api_key:
    print('GEMINI_API_KEY not set!')
    exit(1)

genai.configure(api_key=api_key)
try:
    models = list(genai.list_models())
    print('Available Gemini models:')
    for m in models:
        print(f'- {m.name} | Supported: {m.supported_generation_methods}')
    # Try a simple prompt with the default model
    model_name = os.getenv('GEMINI_MODEL', 'gemini-2.5-flash')
    print(f'\nTesting model: {model_name}')
    model = genai.GenerativeModel(model_name)
    response = model.generate_content('Say hello!')
    print('Model response:', response.text)
except Exception as e:
    print('Error:', e)
