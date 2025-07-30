// Dans Controllers/ForgotPasswordController.cs
using CoworkingApp.Data;
using CoworkingApp.Models;
using CoworkingApp.Services;
using Microsoft.AspNetCore.Mvc;
using Microsoft.EntityFrameworkCore;
using Microsoft.Extensions.Configuration;
using System.Security.Cryptography;

[Route("api/[controller]")]
[ApiController]
public class ForgotPasswordController : ControllerBase
{
    private readonly ApplicationDbContext _context;
    private readonly IEmailService _emailService;
    private readonly ILogger<ForgotPasswordController> _logger;
    private readonly IConfiguration _configuration;

    public ForgotPasswordController(
      ApplicationDbContext context,
      IEmailService emailService,
      ILogger<ForgotPasswordController> logger,
      IConfiguration configuration)
    {
        _context = context;
        _emailService = emailService;
        _logger = logger;
        _configuration = configuration;
    }

    [HttpPost]
    public async Task<IActionResult> Post([FromBody] ForgotPasswordRequest request)
    {
        // Validation approfondie
        if (request == null)
        {
            return BadRequest("La requête ne peut pas être vide");
        }

        if (string.IsNullOrWhiteSpace(request.Email))
        {
            return BadRequest("L'email est requis");
        }

        try
        {
            var user = await _context.Users
                .FirstOrDefaultAsync(u => u.Email == request.Email);

            if (user == null)
            {
                // Pour des raisons de sécurité, ne révélez pas si l'email existe
                return Ok("Si l'email existe, un lien de réinitialisation a été envoyé.");
            }

            // Génération du token
            user.ResetToken = GenerateResetToken();
            user.ResetTokenExpires = DateTime.UtcNow.AddHours(1);

            await _context.SaveChangesAsync();

            // Envoi de l'email
            await SendResetEmail(user.Email, user.ResetToken);

            return Ok("Si l'email existe, un lien de réinitialisation a été envoyé.");
        }
        catch (Exception ex)
        {
            // Loggez l'erreur pour le débogage
            _logger.LogError(ex, "Erreur lors de la demande de réinitialisation");
            return StatusCode(500, "Une erreur interne est survenue");
        }
    }

    private string GenerateResetToken()
    {
        return Convert.ToHexString(RandomNumberGenerator.GetBytes(32));
    }

    private async Task SendResetEmail(string email, string token)
    {
        var resetLink = $"{_configuration["Frontend:BaseUrl"]}/reset-password?token={token}&email={email}";
        var emailBody = $"<p>Cliquez <a href='{resetLink}'>ici</a> pour réinitialiser votre mot de passe.</p>";

        await _emailService.SendEmailAsync(
            email,
            "Réinitialisation de votre mot de passe",
            emailBody);
    }
}