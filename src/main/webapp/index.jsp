<%@ page contentType="text/html;charset=UTF-8" language="java" %>
<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
<!DOCTYPE html>
<html lang="es">
<head>
    <meta charset="UTF-8">
    <meta name="viewport" content="width=device-width, initial-scale=1.0">
    <title>VACU-TRACK - Iniciar Sesión</title>
    <style>
        body {
            font-family: Arial, sans-serif;
            max-width: 400px;
            margin: 100px auto;
            padding: 20px;
            background-color: #f5f5f5;
        }
        .container {
            background: white;
            padding: 30px;
            border-radius: 10px;
            box-shadow: 0 2px 10px rgba(0,0,0,0.1);
        }
        .logo {
            text-align: center;
            margin-bottom: 30px;
        }
        .logo h1 {
            color: #2c5c7a;
            margin: 0;
        }
        .logo p {
            color: #666;
            margin: 5px 0;
            font-style: italic;
        }
        .form-group {
            margin-bottom: 20px;
        }
        label {
            display: block;
            margin-bottom: 5px;
            font-weight: bold;
            color: #333;
        }
        input[type="text"], input[type="password"], select {
            width: 100%;
            padding: 10px;
            border: 2px solid #ddd;
            border-radius: 5px;
            font-size: 16px;
            box-sizing: border-box;
        }
        input:focus, select:focus {
            outline: none;
            border-color: #4CAF50;
        }
        .btn {
            width: 100%;
            padding: 12px;
            background-color: #4CAF50;
            color: white;
            border: none;
            border-radius: 5px;
            font-size: 16px;
            cursor: pointer;
            margin-bottom: 10px;
        }
        .btn:hover {
            background-color: #45a049;
        }
        .btn-secondary {
            background-color: #2196F3;
        }
        .btn-secondary:hover {
            background-color: #1976D2;
        }
        .error {
            background-color: #f8d7da;
            color: #721c24;
            padding: 10px;
            border-radius: 5px;
            margin-bottom: 20px;
            border: 1px solid #f5c6cb;
        }
        .success {
            background-color: #d4edda;
            color: #155724;
            padding: 10px;
            border-radius: 5px;
            margin-bottom: 20px;
            border: 1px solid #c3e6cb;
        }
        .links {
            text-align: center;
            margin-top: 20px;
        }
        .links a {
            color: #2196F3;
            text-decoration: none;
        }
        .links a:hover {
            text-decoration: underline;
        }
    </style>
</head>
<body>
<div class="container">
    <!-- Logo y título -->
    <div class="logo">
        <h1>🩹 VACU-TRACK</h1>
        <p>"Vacunarte es cuidarte"</p>
    </div>

    <!-- Mensajes de error o éxito -->
    <c:if test="${not empty error}">
        <div class="error">
            ⚠️ ${error}
        </div>
    </c:if>

    <c:if test="${not empty success}">
        <div class="success">
            ✅ ${success}
        </div>
    </c:if>

    <!-- Formulario de login -->
    <form method="post" action="${pageContext.request.contextPath}/login">

        <!-- Tipo de usuario -->
        <div class="form-group">
            <label for="tipoUsuario">Tipo de Usuario:</label>
            <select id="tipoUsuario" name="tipoUsuario" required>
                <option value="">Seleccionar tipo...</option>
                <option value="PADRE" ${param.tipoUsuario == 'PADRE' ? 'selected' : ''}>
                    👨‍👩‍👧‍👦 Padre/Madre de Familia
                </option>
                <option value="PROFESIONAL" ${param.tipoUsuario == 'PROFESIONAL' ? 'selected' : ''}>
                    👩‍⚕️ Profesional de Enfermería
                </option>
            </select>
        </div>

        <!-- Cédula -->
        <div class="form-group">
            <label for="cedula">Cédula de Identidad:</label>
            <input type="text"
                   id="cedula"
                   name="cedula"
                   placeholder="Ej: 1234567890"
                   value="${cedula}"
                   maxlength="10"
                   pattern="[0-9]{10}"
                   required>
        </div>

        <!-- Contraseña -->
        <div class="form-group">
            <label for="password">Contraseña:</label>
            <input type="password"
                   id="password"
                   name="password"
                   placeholder="Ingrese su contraseña"
                   required>
        </div>

        <!-- Botón de login -->
        <button type="submit" class="btn">
            🔐 Iniciar Sesión
        </button>

    </form>

    <!-- Enlaces adicionales -->
    <div class="links">
        <p>
            ¿No tienes cuenta?
            <a href="${pageContext.request.contextPath}/registro">
                📝 Regístrate aquí
            </a>
        </p>
    </div>

    <!-- Información adicional -->
    <div style="margin-top: 30px; padding: 15px; background-color: #e7f3ff; border-radius: 5px; font-size: 14px;">
        <strong>ℹ️ Información del Sistema:</strong><br>
        • Para <strong>Padres</strong>: Acceso al historial de vacunas de sus niños<br>
        • Para <strong>Profesionales</strong>: Herramientas de registro y seguimiento<br>
        • Datos seguros y confidenciales
    </div>
</div>

<script>
    // Validación básica del lado del cliente
    document.getElementById('cedula').addEventListener('input', function(e) {
        // Solo permitir números
        this.value = this.value.replace(/[^0-9]/g, '');

        // Limitar a 10 dígitos
        if (this.value.length > 10) {
            this.value = this.value.substring(0, 10);
        }
    });

    // Enfocar el primer campo vacío al cargar la página
    window.onload = function() {
        var tipoUsuario = document.getElementById('tipoUsuario');
        var cedula = document.getElementById('cedula');

        if (!tipoUsuario.value) {
            tipoUsuario.focus();
        } else if (!cedula.value) {
            cedula.focus();
        } else {
            document.getElementById('password').focus();
        }
    };
</script>
</body>
</html>