package OCI.BabyShop.security;

import OCI.BabyShop.domain.AuditLog;
import OCI.BabyShop.repository.AuditLogRepository;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

@Component
@RequiredArgsConstructor
public class AuditInterceptor implements HandlerInterceptor {

    private final AuditLogRepository auditLogRepository;

    @Override
    public void afterCompletion(HttpServletRequest request, HttpServletResponse response, Object handler, Exception ex) {
        String method = request.getMethod();
        // On log toutes les actions (ou on pourrait filtrer sur POST/PUT/DELETE)
        
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        String actor = "ANONYMOUS";
        if (auth != null && auth.isAuthenticated() && !auth.getPrincipal().equals("anonymousUser")) {
            actor = auth.getName();
        }

        String uri = request.getRequestURI();
        int status = response.getStatus();
        
        AuditLog log = AuditLog.builder()
                .actor(actor)
                .action(method)
                .resourceTarget(uri)
                .ipAddress(request.getRemoteAddr())
                .details("Status: " + status + (ex != null ? ", Error: " + ex.getMessage() : ""))
                .build();

        auditLogRepository.save(log);
    }
}
