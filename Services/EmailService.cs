using CoworkingApp.Services;
using System.Net.Mail;
using System.Net;

public class EmailService : IEmailService
{
    private readonly string _smtpServer;
    private readonly int _smtpPort;
    private readonly string _fromEmail;
    private readonly string _smtpUsername;
    private readonly string _smtpPassword;

    public EmailService(IConfiguration configuration)
    {
        _smtpServer = configuration["Email:Smtp"];
        _smtpPort = int.Parse(configuration["Email:SmtpPort"]);
        _fromEmail = configuration["Email:FromEmail"];
        _smtpUsername = configuration["Email:Username"];
        _smtpPassword = configuration["Email:Password"];

        if (string.IsNullOrWhiteSpace(_fromEmail))
            throw new ArgumentNullException(nameof(_fromEmail), "FromEmail is not configured in appsettings.json");
    }

    public async Task SendEmailAsync(string toEmail, string subject, string htmlContent)
    {
        if (string.IsNullOrWhiteSpace(toEmail))
            throw new ArgumentNullException(nameof(toEmail), "Recipient email address cannot be null or empty.");

        using var client = new SmtpClient(_smtpServer, _smtpPort)
        {
            Credentials = new NetworkCredential(_smtpUsername, _smtpPassword),
            EnableSsl = true
        };

        var mailMessage = new MailMessage
        {
            From = new MailAddress(_fromEmail),
            Subject = subject,
            Body = htmlContent,
            IsBodyHtml = true
        };

        mailMessage.To.Add(toEmail);
        await client.SendMailAsync(mailMessage);
    }
}
