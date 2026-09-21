/* API reutilizable de alertas de la app. */

// Mostrar un cuadro de confirmación antes de realizar una acción
function confirmarAccion({
    titulo = '¿Deseas continuar?',
    mensaje = '',
    textoConfirmar = 'Confirmar',
    textoCancelar = 'Cancelar',
    colorConfirmar = '#0d6efd'
} = {}) {
    return Swal.fire({
        icon: 'warning',
        title: titulo,
        text: mensaje,
        showCancelButton: true,
        confirmButtonText: textoConfirmar,
        cancelButtonText: textoCancelar,
        confirmButtonColor: colorConfirmar,
        reverseButtons: true
    });
}

// Mostrar un mensaje de éxito usando SweetAlert2
function mostrarExito(mensaje, titulo = 'Éxito') {
    return Swal.fire({
        icon: 'success',
        title: titulo,
        text: mensaje,
        confirmButtonColor: '#198754',
        confirmButtonText: 'Aceptar'
    });
}

// Mostrar un mensaje de error usando SweetAlert2
function mostrarError(mensaje, titulo = 'Error') {
    return Swal.fire({
        icon: 'error',
        title: titulo,
        text: mensaje,
        confirmButtonColor: '#dc3545',
        confirmButtonText: 'Aceptar'
    });
}

// Mostrar un mensaje de información usando SweetAlert2
function mostrarInformacion(mensaje, titulo = 'Información') {
    return Swal.fire({
        icon: 'info',
        title: titulo,
        text: mensaje,
        confirmButtonText: 'Aceptar'
    });
}
