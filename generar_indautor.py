import os
import glob
from pathlib import Path
import html
import datetime

# Configuraciones
ROOT_DIR = "."
OUTPUT_FILE = "documento_indautor.html"
EVIDENCIA_DIR = "evidencia"

# Extensiones de archivos a procesar
EXTENSIONS = {".php", ".js", ".css", ".html", ".kt", ".java", ".xml", ".sql"}

# Carpetas a ignorar
IGNORE_DIRS = {
    ".git", "node_modules", "vendor", "evidencia", ".idea", "__pycache__",
    "build", ".gradle", "gradle", "androidTest", "test"
}

# Archivos específicos a ignorar
IGNORE_FILES = {
    "local.properties", "gradlew", "gradlew.bat", "settings.gradle.kts",
    "build.gradle.kts", "settings.gradle", "build.gradle", "proguard-rules.pro",
    "gradle.properties", "documento_indautor.html", "generar_indautor.py"
}

def get_description(file_path):
    # Devuelve una descripcion basica, la cual puedes adaptar.
    filename = os.path.basename(file_path)
    return f"Componente lógico correspondiente a {filename}. Contiene la estructura y sintaxis necesaria para la correcta ejecución de esta sección dentro del sistema."

def process_file(file_path):
    try:
        with open(file_path, "r", encoding="utf-8") as f:
            lines = f.readlines()
            
        if not lines:
            return None
            
        if len(lines) > 50:
            first_part = "".join(lines[:25])
            last_part = "".join(lines[-25:])
            
            # Escapar caracteres
            first_part_esc = html.escape(first_part)
            last_part_esc = html.escape(last_part)
            
            codigo = (
                first_part_esc + 
                "\n<span class='omission-badge'>/* ... [ Código extenso: Se omiten líneas intermedias. Mostrando primeras y últimas 25 líneas ] ... */</span>\n\n" + 
                last_part_esc
            )
        else:
            codigo = html.escape("".join(lines))
            
        return codigo
    except Exception as e:
        return None

def generate_html():
    current_year = datetime.datetime.now().year
    
    html_content = [f"""<!DOCTYPE html>
<html lang="es">
<head>
    <title>Código Fuente - Registro de Obra</title>
    <!-- Incluir fuente de Google Fonts para un aspecto mas limpio -->
    <link href="https://fonts.googleapis.com/css2?family=Inter:wght@400;500;600;700&family=Fira+Code:wght@400;500&display=swap" rel="stylesheet">
    
    <!-- Incluir Highlight.js para colorear el codigo de manera profesional (tema claro) -->
    <link rel="stylesheet" href="https://cdnjs.cloudflare.com/ajax/libs/highlight.js/11.9.0/styles/github.min.css">
    
    <style>
        :root {{
            --primary-color: #2563eb; /* Azul corporativo */
            --border-color: #e2e8f0;
            --text-main: #1e293b;
            --text-muted: #64748b;
            --bg-code: #f8fafc;
        }}
        
        body {{
            font-family: 'Inter', system-ui, -apple-system, sans-serif;
            font-size: 11pt; /* Tamaño óptimo para impresión */
            color: var(--text-main);
            margin: 0;
            padding: 0;
            background-color: #fff;
            line-height: 1.5;
        }}
        
        /* Contenedor de página para impresión */
        .page {{
            page-break-after: always;
            padding: 2cm 1.5cm;
            position: relative;
            box-sizing: border-box;
            width: 100%;
            max-width: 21cm; /* Ancho A4 */
            margin: 0 auto;
        }}
        
        /* Cabecera del documento */
        .doc-header {{
            display: flex;
            justify-content: space-between;
            align-items: border-bottom;
            border-bottom: 2px solid var(--primary-color);
            padding-bottom: 10px;
            margin-bottom: 20px;
        }}
        
        .doc-title {{
            font-size: 10pt;
            font-weight: 600;
            color: var(--primary-color);
            text-transform: uppercase;
            letter-spacing: 0.5px;
        }}
        
        .doc-meta {{
            font-size: 9pt;
            color: var(--text-muted);
            text-align: right;
        }}
        
        /* Información del Archivo (Metadatos) */
        .file-info-box {{
            background-color: #f1f5f9;
            border: 1px solid var(--border-color);
            border-left: 4px solid var(--primary-color);
            border-radius: 4px;
            padding: 15px;
            margin-bottom: 20px;
        }}
        
        .info-row {{
            margin-bottom: 8px;
            display: flex;
        }}
        
        .info-row:last-child {{
            margin-bottom: 0;
        }}
        
        .info-label {{
            font-weight: 600;
            width: 100px;
            flex-shrink: 0;
            color: var(--text-main);
        }}
        
        .info-value {{
            flex-grow: 1;
            color: #334155;
        }}
        
        /* Área de Código */
        .code-section-title {{
            font-size: 11pt;
            font-weight: 600;
            margin-bottom: 10px;
            color: var(--text-main);
            display: flex;
            align-items: center;
            gap: 8px;
        }}
        
        .code-container {{
            border: 1px solid var(--border-color);
            border-radius: 6px;
            overflow: hidden;
            background-color: var(--bg-code);
        }}
        
        pre {{
            margin: 0;
            padding: 15px;
            font-family: 'Fira Code', 'Courier New', Courier, monospace;
            font-size: 8.5pt; /* Ligeramente más pequeño para que quepa bien en impresión */
            line-height: 1.4;
            white-space: pre-wrap;
            word-wrap: break-word;
            tab-size: 4;
        }}
        
        /* Etiqueta de omisión */
        .omission-badge {{
            display: block;
            background-color: #fffbeb;
            color: #b45309;
            padding: 6px 10px;
            border-left: 3px solid #f59e0b;
            margin: 10px 0;
            font-family: 'Inter', sans-serif;
            font-size: 9pt;
            font-style: italic;
            text-align: center;
        }}
        
        /* Evidencias */
        .evidencia-grid {{
            display: flex;
            flex-direction: column;
            gap: 30px;
            margin-top: 20px;
        }}
        
        .evidencia-item {{
            text-align: center;
            page-break-inside: avoid;
            border: 1px solid var(--border-color);
            padding: 15px;
            border-radius: 8px;
            background: #f8fafc;
        }}
        
        .evidencia-img {{
            max-width: 100%;
            height: auto;
            border: 1px solid #cbd5e1;
            border-radius: 4px;
            box-shadow: 0 4px 6px -1px rgba(0, 0, 0, 0.1);
        }}
        
        .evidencia-caption {{
            margin-top: 10px;
            font-size: 10pt;
            color: var(--text-muted);
            font-style: italic;
        }}
        
        /* Ajustes específicos para impresión */
        @media print {{
            body {{
                background-color: transparent;
            }}
            .page {{
                margin: 0;
                padding: 1cm 1.5cm;
                border: none;
                box-shadow: none;
                max-width: none;
                width: 100%;
            }}
            /* Evitar que bloques de código se corten a la mitad si es posible */
            .code-container {{
                page-break-inside: auto;
            }}
            pre {{
                white-space: pre-wrap;
                word-wrap: break-word;
            }}
            /* En pantallas normales mostrar como documento centrado */
        }}
        
        @media screen {{
            body {{
                background-color: #e2e8f0;
                padding: 40px 0;
            }}
            .page {{
                background-color: #fff;
                box-shadow: 0 10px 15px -3px rgba(0, 0, 0, 0.1), 0 4px 6px -2px rgba(0, 0, 0, 0.05);
                margin-bottom: 40px;
                border-radius: 4px;
            }}
        }}
    </style>
</head>
<body>
"""]

    if not os.path.exists(EVIDENCIA_DIR):
        os.makedirs(EVIDENCIA_DIR)

    file_count = 0
    # Procesar todos los archivos de código
    for root, dirs, files in os.walk(ROOT_DIR):
        dirs[:] = [d for d in dirs if d not in IGNORE_DIRS]
        
        for file in files:
            ext = os.path.splitext(file)[1].lower()
            if ext in EXTENSIONS:
                file_path = os.path.join(root, file)
                clean_path = file_path.replace("\\", "/")
                if clean_path.startswith("./"):
                    clean_path = clean_path[2:]
                
                if file in IGNORE_FILES:
                    continue
                    
                if clean_path == OUTPUT_FILE or clean_path == os.path.basename(__file__):
                    continue
                    
                codigo = process_file(file_path)
                if codigo:
                    file_count += 1
                    desc = get_description(clean_path)
                    
                    # Detectar lenguaje para Highlight.js
                    lang_class = ""
                    if ext == '.php': lang_class = 'language-php'
                    elif ext == '.js': lang_class = 'language-javascript'
                    elif ext == '.html': lang_class = 'language-xml'
                    elif ext == '.css': lang_class = 'language-css'
                    elif ext == '.sql': lang_class = 'language-sql'
                    elif ext == '.kt': lang_class = 'language-kotlin'
                    elif ext == '.java': lang_class = 'language-java'
                    
                    html_content.append(f'''
    <div class="page">
        <!-- Cabecera Institucional -->
        <div class="doc-header">
            <div class="doc-title">Registro de Obra</div>
            <div class="doc-meta">Anexo Técnico &bull; {current_year}</div>
        </div>
        
        <!-- Bloque de Metadatos -->
        <div class="file-info-box">
            <div class="info-row">
                <div class="info-label">Ruta / Archivo:</div>
                <div class="info-value"><strong>{clean_path}</strong></div>
            </div>
            <div class="info-row">
                <div class="info-label">Descripción:</div>
                <div class="info-value">{desc}</div>
            </div>
        </div>
        
        <!-- Bloque de Código -->
        <div class="code-section-title">
            <svg xmlns="http://www.w3.org/2000/svg" width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"><polyline points="16 18 22 12 16 6"></polyline><polyline points="8 6 2 12 8 18"></polyline></svg>
            Código Fuente
        </div>
        <div class="code-container">
            <pre><code class="{lang_class}">{codigo}</code></pre>
        </div>
    </div>
''')

    # Procesar imágenes de evidencia
    has_images = False
    images_html = ""
    if os.path.exists(EVIDENCIA_DIR):
        for img_file in sorted(os.listdir(EVIDENCIA_DIR)):
            img_path = os.path.join(EVIDENCIA_DIR, img_file)
            if os.path.isfile(img_path) and img_file.lower().endswith(('.png', '.jpg', '.jpeg', '.gif')):
                has_images = True
                images_html += f'''
            <div class="evidencia-item">
                <img src="{img_path}" class="evidencia-img" alt="Evidencia: {img_file}">
                <div class="evidencia-caption">Figura: Captura de pantalla de la interfaz ({img_file})</div>
            </div>'''
                
    if has_images:
        html_content.append(f'''
    <div class="page">
        <div class="doc-header">
            <div class="doc-title">Evidencias Visuales</div>
            <div class="doc-meta">Anexo Técnico &bull; {current_year}</div>
        </div>
        
        <div class="file-info-box" style="border-left-color: #10b981;">
            <div class="info-row">
                <div class="info-label" style="width: 140px;">Sección:</div>
                <div class="info-value"><strong>Anexo de Interfaz Gráfica (Pantallas)</strong></div>
            </div>
            <div class="info-row">
                <div class="info-label" style="width: 140px;">Descripción:</div>
                <div class="info-value">Capturas de pantalla que demuestran el funcionamiento, diseño y la experiencia de usuario del sistema.</div>
            </div>
        </div>
        
        <div class="evidencia-grid">
            {images_html}
        </div>
    </div>
''')

    # Script para activar Highlight.js
    html_content.append("""
    <!-- Cargar Highlight.js para dar formato profesional al código -->
    <script src="https://cdnjs.cloudflare.com/ajax/libs/highlight.js/11.9.0/highlight.min.js"></script>
    <script src="https://cdnjs.cloudflare.com/ajax/libs/highlight.js/11.9.0/languages/kotlin.min.js"></script>
    <script>
        document.addEventListener('DOMContentLoaded', (event) => {
            document.querySelectorAll('pre code').forEach((el) => {
                // No resaltar la etiqueta de omisión
                const originalHTML = el.innerHTML;
                const tempDiv = document.createElement('div');
                tempDiv.innerHTML = originalHTML;
                
                // Extraer el contenido para resaltarlo (sin el badge)
                hljs.highlightElement(el);
            });
        });
    </script>
</body>
</html>
""")

    with open(OUTPUT_FILE, "w", encoding="utf-8") as f:
        f.write("".join(html_content))
    print(f"Documento HTML profesional generado en: {OUTPUT_FILE}")
    print(f"Archivos procesados: {file_count}")

if __name__ == "__main__":
    generate_html()
