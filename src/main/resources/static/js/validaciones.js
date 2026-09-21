function limpiarValidacionesFormulario(formulario) {
    if (!formulario) return;

    formulario.classList.remove('was-validated');
    formulario.querySelectorAll('.is-invalid, .is-valid').forEach(campo => {
        campo.classList.remove('is-invalid', 'is-valid');
    });
    formulario.querySelectorAll('.invalid-tooltip, .invalid-feedback').forEach(mensaje => {
        mensaje.classList.remove('d-block');
    });
}
