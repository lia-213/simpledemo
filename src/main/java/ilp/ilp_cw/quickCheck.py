import google.generativeai as genai
import os

# Make sure your API key is set
genai.configure(api_key="AIzaSyCUwVcSFEIR0-HOteQBliceBNaNjT9ZeMk")

print("List of available models:")
for m in genai.list_models():
    if 'generateContent' in m.supported_generation_methods:
        print(m.name)