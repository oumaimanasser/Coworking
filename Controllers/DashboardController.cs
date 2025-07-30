using Microsoft.AspNetCore.Authorization;
using Microsoft.AspNetCore.Mvc;

namespace CoworkingApp.Controllers
{
    [Route("api/[controller]")]
    [ApiController]
    public class DashboardController : ControllerBase
    {
        // 🟢 Route publique (pas besoin de token)
        [HttpGet("public")]
        public IActionResult PublicData()
        {
            return Ok("Données accessibles sans authentification.");
        }

        // 🔐 Route protégée (n'importe quel utilisateur connecté)
        [Authorize]
        [HttpGet("protected")]
        public IActionResult ProtectedData()
        {
            var username = User.Identity?.Name;
            return Ok($"Bonjour {username}, vous êtes connecté !");
        }

        // 🔒 Route protégée par rôle : Admin uniquement
        [Authorize(Roles = "Admin")]
        [HttpGet("admin-only")]
        public IActionResult AdminData()
        {
            return Ok("Données réservées à l'administrateur !");
        }

        // 🔒 Route protégée par rôle : Client uniquement
        [Authorize(Roles = "Client")]
        [HttpGet("client-only")]
        public IActionResult ClientData()
        {
            return Ok("Bienvenue Client ! Voici vos données.");
        }
    }
}
