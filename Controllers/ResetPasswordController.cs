// Dans Controllers/ResetPasswordController.cs
using CoworkingApp.Data;
using Microsoft.AspNetCore.Mvc;
using Microsoft.EntityFrameworkCore;
using CoworkingApp.Models;
using System.Threading.Tasks;
[Route("api/[controller]")]
[ApiController]
public class ResetPasswordController : ControllerBase
{
    private readonly ApplicationDbContext _context;

    public ResetPasswordController(ApplicationDbContext context)
    {
        _context = context;
    }

    [HttpPost]
    public async Task<IActionResult> Post([FromBody] ResetPasswordRequest request)
    {
        var user = await _context.Users.FirstOrDefaultAsync(u =>
            u.Email == request.Email &&
            u.ResetToken == request.Token &&
            u.ResetTokenExpires > DateTime.UtcNow);

        if (user == null) return BadRequest("Lien invalide ou expiré.");

        // Hacher le nouveau mot de passe
        user.Password = BCrypt.Net.BCrypt.HashPassword(request.NewPassword);
        user.ResetToken = null;
        user.ResetTokenExpires = null;

        await _context.SaveChangesAsync();

        return Ok("Mot de passe réinitialisé avec succès.");
    }
}


