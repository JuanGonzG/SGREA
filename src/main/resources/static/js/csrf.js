// Funcion para agregar el token CSRF a las solicitudes fetch
(function () {
    // Guardar la referencia original de fetch
    const originalFetch = window.fetch.bind(window);
    // Métodos HTTP seguros que no requieren token CSRF
    const safeMethods = new Set(['GET', 'HEAD', 'OPTIONS', 'TRACE']);
    // Función para obtener la configuración CSRF desde las meta etiquetas
    function csrfConfiguration() {
        return {
            // Obtener el token CSRF y el nombre del encabezado desde las meta etiquetas
            token: document.querySelector('meta[name="_csrf"]')?.getAttribute('content'),
            // Obtener el nombre del encabezado CSRF desde las meta etiquetas
            headerName: document.querySelector('meta[name="_csrf_header"]')?.getAttribute('content')
        };
    }
    // Exponer un objeto global para acceder al token CSRF y al nombre del encabezado
    window.sgreaCsrf = {
        // Obtener el token CSRF
        getToken: function () {
            return csrfConfiguration().token;
        },
        // Obtener el nombre del encabezado CSRF
        getHeaderName: function () {
            return csrfConfiguration().headerName;
        },
        // Aplicar el token CSRF a los encabezados de la solicitud
        apply: function (headers) {
            // Obtener la configuración CSRF
            const config = csrfConfiguration();
            // Si el token y el nombre del encabezado existen, agregarlos a los encabezados
            if (config.token && config.headerName) {
                headers.set(config.headerName, config.token);
            }
            return headers;
        }
    };
    // Sobrescribir la función fetch para agregar el token CSRF a las solicitudes no seguras
    window.fetch = function (input, init) {
        // Obtener las opciones de la solicitud
        const options = init || {};
        // Determinar el método de la solicitud (GET, POST, etc.)
        const requestMethod = options.method || (input instanceof Request ? input.method : 'GET');
        // Convertir el método a mayúsculas para compararlo con los métodos seguros
        const method = requestMethod.toUpperCase();
        // Si el método es seguro, llamar a la función fetch original sin modificar los encabezados
        if (safeMethods.has(method)) {
            return originalFetch(input, options);
        }
        // Crear un objeto Headers a partir de las opciones de la solicitud o de los encabezados de la solicitud original
        const headers = new Headers(options.headers || (input instanceof Request ? input.headers : undefined));
        // Aplicar el token CSRF a los encabezados
        window.sgreaCsrf.apply(headers);
        // Llamar a la función fetch original con los encabezados modificados
        return originalFetch(input, Object.assign({}, options, {headers}));
    };
})();
